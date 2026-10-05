package io.github.josemodi97.sageactive4j.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.FileUpload;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ModelParsingTest {

    @Test
    void datesAcceptBothDateAndDateTimeForms() {
        assertEquals(LocalDate.of(2026, 10, 5), SageObject.parseDate("2026-10-05"));
        assertEquals(LocalDate.of(2026, 10, 5), SageObject.parseDate("2026-10-05T00:00:00+02:00"));
        assertNull(SageObject.parseDate("garbage"));
        assertNull(SageObject.parseDate(null));

        assertEquals(OffsetDateTime.of(2025, 9, 18, 9, 41, 12, 0, ZoneOffset.UTC), SageObject.parseDateTime("2025-09-18T09:41:12Z"));
        assertEquals(OffsetDateTime.of(2025, 9, 18, 9, 41, 12, 0, ZoneOffset.UTC), SageObject.parseDateTime("2025-09-18T09:41:12"));
        assertEquals(OffsetDateTime.of(2025, 9, 18, 0, 0, 0, 0, ZoneOffset.UTC), SageObject.parseDateTime("2025-09-18"));
    }

    @Test
    void missingFieldsAreNullNotErrors() {
        Customer customer = new Customer(Collections.<String, Object>emptyMap());
        assertNull(customer.getCode());
        assertNull(customer.getMainAddress());
        assertTrue(customer.getContacts().isEmpty());
        assertNull(new OpenItem(Collections.<String, Object>emptyMap()).getOutstandingAmount());
    }

    @Test
    void rawAccessForUnmodelledFields() {
        Customer customer = new Customer(JsonReader.parseObject("{\"eInvoicingAddress\":\"123456782_Accounting\",\"x\":{\"y\":1}}"));
        assertEquals("123456782_Accounting", customer.get("eInvoicingAddress"));
        assertEquals(1L, customer.get("x", "y"));
        assertTrue(customer.getRaw().containsKey("x"));
    }

    @Test
    void connectionsParseEdgesAsWellAsNodes() {
        Connection<SageFile> c = Connection.fromJson(JsonReader.parseObject(
                "{\"edges\":[{\"cursor\":\"x\",\"node\":{\"id\":\"f1\"}}],\"pageInfo\":{\"hasNextPage\":true,\"endCursor\":\"x\"}}"),
                SageFile::new);
        assertEquals("f1", c.getNodes().get(0).getId());
        assertNull(c.getTotalCount());
        assertEquals("x", c.getPageInfo().getEndCursor());
    }

    @Test
    void equalityFollowsTheJson() {
        assertEquals(new Product(JsonReader.parseObject("{\"id\":\"p\"}")), new Product(JsonReader.parseObject("{\"id\":\"p\"}")));
    }

    @Test
    void uploadContentTypesAreGuessedFromTheExtension() {
        assertEquals("application/pdf", FileUpload.of("A.PDF", new byte[0]).getContentType());
        assertEquals("image/jpeg", FileUpload.of("scan.jpeg", new byte[0]).getContentType());
        assertEquals("application/octet-stream", FileUpload.of("blob", new byte[0]).getContentType());
        assertEquals("text/x-custom", FileUpload.of("a.pdf", "text/x-custom", new byte[0]).getContentType());
    }

    @Test
    void newDomainModelsParseCorrectly() {
        SalesQuote quote = new SalesQuote(JsonReader.parseObject("{\"id\":\"q1\",\"operationalNumber\":\"Q100\","
                + "\"status\":\"Pending\",\"lines\":[{\"id\":\"ql1\",\"productId\":\"p1\",\"unitPrice\":99.50}]}"));
        assertEquals("q1", quote.getId());
        assertEquals("Q100", quote.getOperationalNumber());
        assertEquals("Pending", quote.getStatus());
        assertEquals(1, quote.getLines().size());
        assertEquals(new java.math.BigDecimal("99.50"), quote.getLines().get(0).getUnitPrice());

        SalesOrder order = new SalesOrder(JsonReader.parseObject("{\"id\":\"o1\",\"operationalNumber\":\"ORD100\","
                + "\"status\":\"Closed\",\"totalLiquid\":120.00}"));
        assertEquals("o1", order.getId());
        assertEquals("ORD100", order.getOperationalNumber());
        assertEquals("Closed", order.getStatus());
        assertEquals(new java.math.BigDecimal("120.00"), order.getTotalLiquid());

        SalesTariff tariff = new SalesTariff(JsonReader.parseObject("{\"id\":\"t1\",\"code\":\"TAR01\",\"enabled\":true}"));
        assertEquals("TAR01", tariff.getCode());
        assertTrue(tariff.isEnabled());

        PaymentTerm term = new PaymentTerm(JsonReader.parseObject("{\"id\":\"pt1\",\"name\":\"60 Days\","
                + "\"lines\":[{\"day\":60,\"type\":\"MATURITY\"}]}"));
        assertEquals("60 Days", term.getName());
        assertEquals(60, term.getLines().get(0).getDay());

        Tax tax = new Tax(JsonReader.parseObject("{\"name\":\"TVA 20%\",\"percentage\":20,\"inactive\":false}"));
        assertEquals("TVA 20%", tax.getName());
        assertEquals(new java.math.BigDecimal("20"), tax.getPercentage());
        assertEquals(false, tax.isInactive());

        TaxGroup group = new TaxGroup(JsonReader.parseObject("{\"name\":\"Group1\",\"taxGroupCode\":\"G1\"}"));
        assertEquals("Group1", group.getName());
        assertEquals("G1", group.getTaxGroupCode());

        TaxTreatment treatment = new TaxTreatment(JsonReader.parseObject("{\"description\":\"Domestic\",\"taxCode\":\"DOM\"}"));
        assertEquals("Domestic", treatment.getDescription());
        assertEquals("DOM", treatment.getTaxCode());
    }
}
