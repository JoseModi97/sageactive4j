package io.github.josemodi97.sageactive4j.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.exception.SageActive4jGraphQLException;
import io.github.josemodi97.sageactive4j.graphql.FileUpload;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.input.AggregationRequest;
import io.github.josemodi97.sageactive4j.input.FileAttachment;
import io.github.josemodi97.sageactive4j.input.FileEntityType;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import io.github.josemodi97.sageactive4j.model.AggregationDefinition;
import io.github.josemodi97.sageactive4j.model.AggregationResult;
import io.github.josemodi97.sageactive4j.model.FileExportRequest;
import io.github.josemodi97.sageactive4j.testsupport.DomainTestSupport;
import io.github.josemodi97.sageactive4j.testsupport.StubServer.Recorded;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FilesCatalogLocalizationTest extends DomainTestSupport {

    @Test
    void uploadIsAGraphQLMultipartRequestWithThePreflightHeader() {
        respond("{\"uploadFileToEntity\":{\"id\":\"98f7adc7dd104cd984f9c113ef9a8e54.pdf\"}}");

        String id = sage.files().upload(new FileAttachment()
                .file(FileUpload.of("invoice-q1.pdf", "%PDF-1.4 fake".getBytes(StandardCharsets.US_ASCII)))
                .entityType(FileEntityType.ACCOUNTING_ENTRY)
                .entityId("8b21a143-4d9c-41ea-b1dc-1cf9d91a72f1")
                .fileName("invoice-q1")
                .businessDate(LocalDate.of(2025, 4, 4))
                .comment("Invoice for Q1"));

        assertEquals("98f7adc7dd104cd984f9c113ef9a8e54.pdf", id);
        Recorded request = lastRequest();
        assertEquals("1", request.header("GraphQL-preflight"));
        assertEquals(ORG, request.header("X-OrganizationId"));
        String contentType = request.header("Content-Type");
        assertTrue(contentType.startsWith("multipart/form-data; boundary="), contentType);

        Map<String, String> parts = parseMultipart(request.body, contentType.substring(contentType.indexOf('=') + 1));
        Map<String, Object> operations = JsonReader.parseObject(parts.get("operations"));
        Map<String, Object> input = JsonReader.getMap(operations, "variables", "input");
        assertTrue(input.containsKey("file") && input.get("file") == null, "file must be present and null: " + input);
        assertEquals("ACCOUNTING_ENTRY", input.get("entityType"));
        assertEquals("2025-04-04", input.get("businessDate"));
        assertEquals("{\"0\":[\"variables.input.file\"]}", parts.get("map"));
        assertEquals("%PDF-1.4 fake", parts.get("0"));
        assertTrue(parts.get("0.headers").contains("filename=\"invoice-q1.pdf\""), parts.get("0.headers"));
        assertTrue(parts.get("0.headers").contains("Content-Type: application/pdf"), parts.get("0.headers"));
    }

    @Test
    void apAutomationNeedsNoEntityButOthersDo() {
        FileUpload pdf = FileUpload.of("bill.pdf", new byte[] {1});
        new FileAttachment().file(pdf).entityType(FileEntityType.AP_AUTOMATION).validate();
        assertThrows(IllegalArgumentException.class,
                () -> new FileAttachment().file(pdf).entityType(FileEntityType.CUSTOMER).validate());
        assertThrows(IllegalArgumentException.class,
                () -> new FileAttachment().entityType(FileEntityType.AP_AUTOMATION).validate());
    }

    @Test
    void filenamesCannotBreakOutOfTheirHeader() {
        respond("{\"uploadFileToEntity\":{\"id\":\"x.pdf\"}}");
        sage.files().upload(new FileAttachment().entityType(FileEntityType.AP_AUTOMATION)
                .file(FileUpload.of("evil\".pdf\r\nX-Injected: 1", new byte[] {1})));
        assertNull(lastRequest().header("X-Injected"));
        assertFalse(lastRequest().body.contains("\r\nX-Injected"));
    }

    @Test
    void listForAnEntityUsesTheFlatSupportedFilters() {
        respond("{\"files\":{\"edges\":[{\"node\":{\"id\":\"f.pdf\",\"fileName\":\"invoice\",\"size\":2048,\"entityType\":\"CUSTOMER\"}}],"
                + "\"pageInfo\":{\"hasNextPage\":false}}}");

        assertEquals(2048L, sage.files().listFor(FileEntityType.CUSTOMER, "c1",
                ListOptions.first(10).where("{ fileName: { contains: \"invoice\" } }")).getNodes().get(0).getSize());

        String q = query(lastRequest());
        assertContains(q, "where: { entityType: { eq: CUSTOMER }, entityId: { eq: $entityId }, fileName: { contains: \"invoice\" } }");
        assertFalse(q.contains("and:"), "files rejects nested and groups");
        assertContains(q, "order: { businessDate: DESC }");
    }

    @Test
    void exportMatchesTheDocumentedExample() {
        respond("{\"filesExport\":{\"status\":\"ACCEPTED\",\"message\":\"Export request accepted.\",\"requestedAt\":\"2025-09-18T09:41:12Z\"}}");

        FileExportRequest export = sage.files().requestExport("en-US",
                "{ entityType: { eq: CUSTOMER }, businessDate: { gte: \"2024-01-01\", lte: \"2024-12-31\" } }");

        assertTrue(export.isAccepted());
        assertEquals(2025, export.getRequestedAt().getYear());
        assertContains(query(lastRequest()), "filesExport(language: $language, where: { entityType: { eq: CUSTOMER }");
        assertEquals("en-US", variables(lastRequest()).get("language"));
    }

    @Test
    void exportWithoutFilter() {
        respond("{\"filesExport\":{\"status\":\"NO_MATCH\"}}");
        assertFalse(sage.files().requestExport("fr", null).isAccepted());
        assertContains(query(lastRequest()), "filesExport(language: $language) {");
    }

    @Test
    void listExportsFiltersOnTheFlag() {
        respond("{\"files\":{\"nodes\":[],\"pageInfo\":{\"hasNextPage\":false}}}");
        sage.files().listExports(ListOptions.defaults());
        assertContains(query(lastRequest()), "where: { filesExport: { eq: true } }");
    }

    @Test
    void aggregationCatalogMatchesTheDocumentedExample() {
        respond("{\"aggregationCatalog\":{\"definitions\":[{\"entityKey\":\"queryAggregateAccountingEntryLines\","
                + "\"context\":\"Accounting Entry Lines by Account\",\"aggregationTypes\":[\"SUM\",\"AVG\",\"MIN\",\"MAX\",\"DISTINCTCOUNT\",\"COUNT\"],"
                + "\"periodTypes\":[\"Day\",\"Week\",\"Month\",\"Quarter\",\"Year\"],\"filters\":[\"All accounts\",\"Charges\"],"
                + "\"groupBy\":[{\"name\":\"None\",\"title1\":null,\"title2\":null},{\"name\":\"Account Code\",\"title1\":\"Account Code\",\"title2\":\"Account Name\"}],"
                + "\"valueColumns\":[\"Debit\",\"Credit\"],\"distinctCounts\":[\"Accounting Entries\"],\"searchScopes\":[\"CustomerId\"]}]}}");

        List<AggregationDefinition> catalog = sage.catalog().aggregationCatalog("Accounting Entry");

        AggregationDefinition definition = catalog.get(0);
        assertEquals("queryAggregateAccountingEntryLines", definition.getEntityKey());
        assertEquals(6, definition.getAggregationTypes().size());
        assertEquals("Account Name", definition.getGroupBy().get(1).getTitle2());
        assertEquals("Accounting Entry", variables(lastRequest()).get("contextContains"));
        assertNull(lastRequest().header("X-OrganizationId"), "the catalog is organization-independent");
    }

    @Test
    void aggregateSendsTheCatalogValuesVerbatim() {
        respond("{\"aggregationExecute\":{\"meta\":{\"entityKey\":\"queryAggregateSalesInvoices\",\"periodType\":\"Year\"},"
                + "\"rows\":[{\"groupValue\":\"OPALE\",\"secondValue\":\"Opale\",\"periodN\":\"2026\",\"periodN_1\":\"2025\","
                + "\"valueN\":28570,\"valueN_1\":28970,\"deltaPercent\":-1.38}]}}");

        AggregationResult result = sage.catalog().aggregate(new AggregationRequest()
                .entityKey("queryAggregateSalesInvoices").aggregationType("SUM").periodType("Year")
                .dateMin(LocalDate.of(2025, 1, 1)).dateMax(LocalDate.of(2026, 12, 31))
                .groupByName("Customer").compare(true).top(5));

        AggregationResult.Row row = result.getRows().get(0);
        assertEquals("OPALE", row.getGroupValue());
        assertEquals("2025", row.getPeriodN1());
        assertEquals(new BigDecimal("-1.38"), row.getDeltaPercent());
        assertEquals("Year", result.getMeta().get("periodType"));
        Map<String, Object> input = JsonReader.getMap(variables(lastRequest()), "input");
        assertEquals("2025-01-01", input.get("dateMin"));
        assertEquals(Boolean.TRUE, input.get("compare"));
        assertEquals(5L, input.get("top"));
    }

    @Test
    void localizedErrorMessage() {
        respond("{\"localizedErrorMessage\":{\"message\":\"You cannot enter a code that includes blank spaces.\"}}");
        assertEquals("You cannot enter a code that includes blank spaces.",
                sage.localization().errorMessage("sales.businessErrors.codeCannotContainBlankSpaces", "en"));
        assertEquals("sales.businessErrors.codeCannotContainBlankSpaces", variables(lastRequest()).get("errorCode"));
        assertEquals("en", variables(lastRequest()).get("language"));
    }

    @Test
    void explainLocalizesBusinessErrorKeysOnly() {
        respondErrors("[{\"message\":\"global.businessErrors.documentIdAlreadyExistInOrganization\"}]");
        respond("{\"localizedErrorMessage\":{\"message\":\"Ce numéro d'identification existe déjà.\"}}");

        SageActive4jGraphQLException e = assertThrows(SageActive4jGraphQLException.class,
                () -> sage.query("mutation { createCustomer(input: {}) { id } }"));
        assertEquals("Ce numéro d'identification existe déjà.", sage.localization().explain(e, "fr"));

        assertTrue(LocalizationClient.isErrorKey("sales.businessErrors.invalidDocumentId"));
        assertFalse(LocalizationClient.isErrorKey("The field 'x' does not exist on the type 'Query'."));
    }

    /** Splits a multipart body into part name → content (and name + ".headers" → raw part headers). */
    private static Map<String, String> parseMultipart(String body, String boundary) {
        Map<String, String> parts = new LinkedHashMap<String, String>();
        assertTrue(body.endsWith("--" + boundary + "--\r\n"), "missing closing boundary");
        for (String chunk : body.split("--" + boundary)) {
            if (chunk.isEmpty() || chunk.startsWith("--")) {
                continue;
            }
            String part = chunk.substring(2, chunk.length() - 2); // strip leading and trailing CRLF
            int split = part.indexOf("\r\n\r\n");
            String headers = part.substring(0, split);
            String name = headers.replaceAll("(?s).*?; name=\"([^\"]*)\".*", "$1");
            parts.put(name, part.substring(split + 4));
            parts.put(name + ".headers", headers);
        }
        return parts;
    }
}
