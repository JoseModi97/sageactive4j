package io.github.josemodi97.sageactive4j.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.exception.InvoiceWorkflowException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jApiException;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.input.OpenItemSettlementInput;
import io.github.josemodi97.sageactive4j.input.SalesInvoiceInput;
import io.github.josemodi97.sageactive4j.input.SalesInvoiceLineInput;
import io.github.josemodi97.sageactive4j.input.SalesOrderInput;
import io.github.josemodi97.sageactive4j.input.SalesOrderLineInput;
import io.github.josemodi97.sageactive4j.input.SalesQuoteInput;
import io.github.josemodi97.sageactive4j.input.SalesQuoteLineInput;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import io.github.josemodi97.sageactive4j.model.AccountingPosting;
import io.github.josemodi97.sageactive4j.model.InvoicePosting;
import io.github.josemodi97.sageactive4j.model.OpenItem;
import io.github.josemodi97.sageactive4j.model.SalesInvoice;
import io.github.josemodi97.sageactive4j.model.SalesOrder;
import io.github.josemodi97.sageactive4j.model.SalesQuote;
import io.github.josemodi97.sageactive4j.testsupport.DomainTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SalesClientTest extends DomainTestSupport {

    // Response shapes below are Sage Active's documented examples.
    private static final String CREATED = "{\"createSalesInvoice\":{\"id\":\"e451aff7-074f-4e72-a6fb-4ea25e9d9c64\"}}";
    private static final String CLOSED = "{\"closeSalesInvoice\":{\"id\":\"e451aff7-074f-4e72-a6fb-4ea25e9d9c64\",\"operationalNumber\":\"0038\"}}";
    private static final String POSTED = "{\"postSalesInvoice\":{\"accountingEntryId\":\"4b4a0742-392b-4170-bfd4-969a2c882f44\","
            + "\"accountingEntryNumber\":1516,\"accountingEntries\":[{\"accountingEntryId\":\"4b4a0742-392b-4170-bfd4-969a2c882f44\","
            + "\"accountingEntryNumber\":1516}]}}";
    private static final String SALES_JOURNAL = "{\"journalTypes\":{\"nodes\":[{\"id\":\"6fdf8926-cfdd-4ef8-8f89-39d6bfa622da\","
            + "\"code\":\"VTE\",\"name\":\"Ventes\",\"type\":\"SALES_INVOICE\",\"deactivated\":false}],"
            + "\"pageInfo\":{\"hasNextPage\":false,\"endCursor\":\"c1\"},\"totalCount\":1}}";

    private static SalesInvoiceInput invoice() {
        return new SalesInvoiceInput()
                .customerId("cust-1")
                .documentDate(LocalDate.of(2023, 12, 5))
                .addLine(new SalesInvoiceLineInput().productId("prod-1").totalQuantity(50)
                        .unitPrice(new BigDecimal("11")).firstDiscount(new BigDecimal("5")))
                .addLine(new SalesInvoiceLineInput().productId("prod-2").totalQuantity(1).unitPrice(new BigDecimal("140")));
    }

    @Test
    void listsInvoicesWithLinesNewestFirst() {
        respond("{\"salesInvoices\":{\"nodes\":[{\"id\":\"inv-1\",\"operationalNumber\":\"0037\",\"documentDate\":\"2026-10-01T00:00:00Z\","
                + "\"status\":\"Posted\",\"type\":\"NORMAL\",\"customer\":{\"id\":\"c1\",\"code\":\"10001\",\"socialName\":\"DUPONT SA\"},"
                + "\"totalNet\":150.00,\"totalLiquid\":180.00,\"lines\":[{\"order\":1,\"productCode\":\"P1\",\"totalQuantity\":2,"
                + "\"unitPrice\":75.00}]}],\"pageInfo\":{\"hasNextPage\":true,\"endCursor\":\"abc\"},\"totalCount\":42}}");

        Connection<SalesInvoice> page = sage.sales().invoices(ListOptions.first(10));

        assertEquals(42L, page.getTotalCount());
        assertTrue(page.hasNextPage());
        SalesInvoice invoice = page.getNodes().get(0);
        assertEquals("0037", invoice.getOperationalNumber());
        assertEquals(LocalDate.of(2026, 10, 1), invoice.getDocumentDate());
        assertEquals("DUPONT SA", invoice.getCustomer().getSocialName());
        assertEquals(new BigDecimal("180.00"), invoice.getTotalLiquid());
        assertEquals(new BigDecimal("75.00"), invoice.getLines().get(0).getUnitPrice());

        String q = query(lastRequest());
        assertContains(q, "salesInvoices(first: $first, after: $after, order: [{ documentDate: DESC }])");
        assertEquals(10L, JsonReader.getLong(variables(lastRequest()), "first"));
        assertEquals(ORG, lastRequest().header("X-OrganizationId"));
    }

    @Test
    void callerFilterAndOrderReplaceTheDefaults() {
        respond("{\"salesInvoices\":{\"nodes\":[],\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":0}}");
        sage.sales().invoices(ListOptions.first(5)
                .where("{ customerId: { eq: $c } }").variable("c", "UUID", "cust-9")
                .order("[{ totalLiquid: DESC }]"));
        String q = query(lastRequest());
        assertContains(q, "query ($first: Int, $after: String, $c: UUID)");
        assertContains(q, "where: { customerId: { eq: $c } }, order: [{ totalLiquid: DESC }]");
        assertEquals("cust-9", variables(lastRequest()).get("c"));
    }

    @Test
    void createInvoiceSendsTheDocumentedShape() {
        respond(CREATED);
        String id = sage.sales().createInvoice(invoice());

        assertEquals("e451aff7-074f-4e72-a6fb-4ea25e9d9c64", id);
        assertContains(query(lastRequest()), "mutation ($values: SalesInvoiceCreateGLDtoInput!) { createSalesInvoice(input: $values)");
        Map<String, Object> values = JsonReader.getMap(variables(lastRequest()), "values");
        assertEquals("cust-1", values.get("customerId"));
        assertEquals("2023-12-05", values.get("documentDate"));
        List<Object> lines = JsonReader.getList(values, "lines");
        assertEquals(2, lines.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) lines.get(0);
        assertEquals(50L, first.get("totalQuantity"));
        assertEquals("11", JsonReader.getString(first, "unitPrice"));
        assertEquals("5", JsonReader.getString(first, "firstDiscount"));
    }

    @Test
    void invalidInputsFailBeforeAnyRequest() {
        assertThrows(IllegalArgumentException.class, () -> sage.sales().createInvoice(new SalesInvoiceInput().customerId("c")));
        assertThrows(IllegalArgumentException.class, () -> new SalesInvoiceInput().addLine(new SalesInvoiceLineInput().unitPrice(BigDecimal.ONE)));
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void closeAndPostUseSalesInvoiceId() {
        respond(CLOSED);
        respond(POSTED);

        SalesInvoice closed = sage.sales().closeInvoice("inv-1");
        AccountingPosting posted = sage.sales().postInvoice("inv-1", "journal-1", "Sales Invoice No. 0007 27/06/2024");

        assertEquals("0038", closed.getOperationalNumber());
        assertEquals("inv-1", JsonReader.getString(variables(request(0)), "input", "salesInvoiceId"));
        Map<String, Object> postInput = JsonReader.getMap(variables(request(1)), "input");
        assertEquals("inv-1", postInput.get("salesInvoiceId"));
        assertEquals("journal-1", postInput.get("journalTypeId"));
        assertEquals("Sales Invoice No. 0007 27/06/2024", postInput.get("accountingEntryDescription"));
        assertEquals(1516L, posted.getAccountingEntryNumber());
        assertEquals(1, posted.getEntries().size());
    }

    @Test
    void createAndPostResolvesTheJournalFirstThenRunsAllThreeSteps() {
        respond(SALES_JOURNAL);
        respond(CREATED);
        respond(CLOSED);
        respond(POSTED);

        InvoicePosting result = sage.sales().createAndPostInvoice(invoice());

        assertEquals("e451aff7-074f-4e72-a6fb-4ea25e9d9c64", result.getInvoiceId());
        assertEquals("0038", result.getOperationalNumber());
        assertEquals(1516L, result.getPosting().getAccountingEntryNumber());

        assertContains(query(request(0)), "where: { type: { eq: SALES_INVOICE }, deactivated: { eq: false } }");
        assertContains(query(request(1)), "createSalesInvoice");
        assertContains(query(request(2)), "closeSalesInvoice");
        Map<String, Object> postInput = JsonReader.getMap(variables(request(3)), "input");
        assertEquals("6fdf8926-cfdd-4ef8-8f89-39d6bfa622da", postInput.get("journalTypeId"));
        assertEquals("Sales invoice 0038", postInput.get("accountingEntryDescription"));
    }

    @Test
    void noSalesJournalFailsBeforeCreatingAnything() {
        respond("{\"journalTypes\":{\"nodes\":[],\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":0}}");
        assertThrows(SageActive4jApiException.class, () -> sage.sales().createAndPostInvoice(invoice()));
        assertEquals(1, server.requests().size(), "no invoice may be created without a journal to post it to");
    }

    @Test
    void closeFailureReportsTheDraftToResume() {
        respond(CREATED);
        respondErrors("[{\"message\":\"sales.businessErrors.mainAddressCityMustBeProvidedWhenClosingSalesInvoice\"}]");

        InvoiceWorkflowException e = assertThrows(InvoiceWorkflowException.class,
                () -> sage.sales().createAndPostInvoice(invoice(), "journal-1", null));

        assertEquals("e451aff7-074f-4e72-a6fb-4ea25e9d9c64", e.getInvoiceId());
        assertEquals(InvoiceWorkflowException.Step.CLOSE, e.getFailedStep());
        assertNull(e.getOperationalNumber());
        assertTrue(e.getMessage().contains("mainAddressCityMustBeProvided"), e.getMessage());
    }

    @Test
    void postFailureReportsTheClosedNumber() {
        respond(CREATED);
        respond(CLOSED);
        respondErrors("[{\"message\":\"accounting.businessErrors.periodClosed\"}]");

        InvoiceWorkflowException e = assertThrows(InvoiceWorkflowException.class,
                () -> sage.sales().createAndPostInvoice(invoice(), "journal-1", "Custom"));

        assertEquals(InvoiceWorkflowException.Step.POST, e.getFailedStep());
        assertEquals("0038", e.getOperationalNumber());
        assertEquals("Custom", JsonReader.getString(variables(request(2)), "input", "accountingEntryDescription"));
    }

    @Test
    void openItemsFilterOnTheInvoice() {
        respond("{\"salesInvoiceOpenItems\":{\"nodes\":[{\"id\":\"oi-1\",\"amount\":500,\"paidAmountAccumulated\":200,"
                + "\"dueDate\":\"2026-11-30T00:00:00Z\",\"status\":\"PARTIAL\",\"salesInvoiceId\":\"inv-1\"}],"
                + "\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");

        List<OpenItem> items = sage.sales().openItems("inv-1");

        assertEquals(new BigDecimal("300"), items.get(0).getOutstandingAmount());
        assertEquals("inv-1", items.get(0).getInvoiceId());
        assertEquals(LocalDate.of(2026, 11, 30), items.get(0).getDueDate());
        assertContains(query(lastRequest()), "where: { salesInvoiceId: { eq: $invoiceId } }");
        assertContains(query(lastRequest()), "$invoiceId: UUID");
        assertEquals("inv-1", variables(lastRequest()).get("invoiceId"));
        assertEquals(500L, JsonReader.getLong(variables(lastRequest()), "first"));
    }

    @Test
    void settlementUsesTheSalesLinkageField() {
        respond("{\"salesOpenItemSettlement\":{\"accountingEntryId\":\"0e1987c3-ed48-4906-8b26-42e4a4c36079\",\"accountingEntryNumber\":818}}");

        AccountingPosting posting = sage.sales().settleOpenItems(new OpenItemSettlementInput()
                .entryDate(LocalDate.of(2024, 1, 1))
                .paymentMethodId("pm-1")
                .thirdPartyId("cust-1")
                .description("Full settlement of Invoice IN0022")
                .pay("oi-1", new BigDecimal("500"))
                .pay("oi-2", new BigDecimal("792")));

        assertEquals(818L, posting.getAccountingEntryNumber());
        Map<String, Object> input = JsonReader.getMap(variables(lastRequest()), "input");
        assertEquals("2024-01-01", input.get("entryDate"));
        assertEquals("pm-1", input.get("paymentMethodId"));
        assertEquals("cust-1", input.get("thirdPartyId"));
        List<Object> paid = JsonReader.getList(input, "salesOpenItemLinkagePaidAmounts");
        assertEquals(2, paid.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> second = (Map<String, Object>) paid.get(1);
        assertEquals("oi-2", second.get("openItemId"));
        assertEquals(792L, second.get("paidAmount"));
    }

    @Test
    void settlementRequiresAPayment() {
        assertThrows(IllegalArgumentException.class, () -> sage.sales().settleOpenItems(new OpenItemSettlementInput()
                .entryDate(LocalDate.now()).paymentMethodId("pm").thirdPartyId("c")));
        assertThrows(IllegalArgumentException.class, () -> new OpenItemSettlementInput().pay("oi", BigDecimal.ZERO));
    }

    @Test
    void quotesAndCreateQuote() {
        respond("{\"salesQuotes\":{\"nodes\":[{\"id\":\"q-1\",\"operationalNumber\":\"Q001\",\"documentDate\":\"2026-10-01T00:00:00Z\","
                + "\"status\":\"Pending\",\"socialName\":\"ACME CORP\",\"totalNet\":250.00,\"lines\":[{\"order\":1,"
                + "\"productId\":\"p-1\",\"productCode\":\"P1\",\"totalQuantity\":5,\"unitPrice\":50.00,\"totalNet\":250.00}]}],"
                + "\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");

        Connection<SalesQuote> page = sage.sales().quotes(ListOptions.first(5));
        assertEquals(1L, page.getTotalCount());
        SalesQuote quote = page.getNodes().get(0);
        assertEquals("Q001", quote.getOperationalNumber());
        assertEquals("Pending", quote.getStatus());
        assertEquals(new BigDecimal("250.00"), quote.getTotalNet());
        assertEquals(1, quote.getLines().size());
        assertEquals(new BigDecimal("50.00"), quote.getLines().get(0).getUnitPrice());

        respond("{\"createSalesQuote\":{\"id\":\"q-new\",\"operationalNumber\":\"Q002\"}}");
        String createdId = sage.sales().createQuote(new SalesQuoteInput()
                .customerId("cust-1")
                .documentDate(LocalDate.of(2026, 10, 2))
                .addLine(new SalesQuoteLineInput().productId("p-1").totalQuantity(10).unitPrice(new BigDecimal("45.00"))));
        assertEquals("q-new", createdId);
        assertContains(query(lastRequest()), "createSalesQuote");
    }

    @Test
    void ordersAndCreateOrder() {
        respond("{\"salesOrders\":{\"nodes\":[{\"id\":\"o-1\",\"operationalNumber\":\"ORD-01\",\"documentDate\":\"2026-10-01T00:00:00Z\","
                + "\"status\":\"Pending\",\"socialName\":\"ACME CORP\",\"totalNet\":300.00,\"lines\":[{\"order\":1,"
                + "\"productId\":\"p-1\",\"productCode\":\"P1\",\"totalQuantity\":6,\"pendingQuantity\":6,"
                + "\"unitPrice\":50.00,\"totalNet\":300.00}]}],"
                + "\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");

        Connection<SalesOrder> page = sage.sales().orders(ListOptions.first(5));
        assertEquals(1L, page.getTotalCount());
        SalesOrder order = page.getNodes().get(0);
        assertEquals("ORD-01", order.getOperationalNumber());
        assertEquals("Pending", order.getStatus());
        assertEquals(new BigDecimal("300.00"), order.getTotalNet());
        assertEquals(1, order.getLines().size());
        assertEquals(new BigDecimal("6"), order.getLines().get(0).getPendingQuantity());

        respond("{\"createSalesOrder\":{\"id\":\"o-new\",\"operationalNumber\":\"ORD-02\"}}");
        String createdId = sage.sales().createOrder(new SalesOrderInput()
                .customerId("cust-1")
                .documentDate(LocalDate.of(2026, 10, 2))
                .addLine(new SalesOrderLineInput().productId("p-1").totalQuantity(12).unitPrice(new BigDecimal("40.00"))));
        assertEquals("o-new", createdId);
        assertContains(query(lastRequest()), "createSalesOrder");
    }

    @Test
    void generateCreditNote() {
        respond("{\"generateCreditNote\":{\"id\":\"cn-1\"}}");
        String creditNoteId = sage.sales().generateCreditNote("inv-123");
        assertEquals("cn-1", creditNoteId);
        assertContains(query(lastRequest()), "generateCreditNote");
        Map<String, Object> input = JsonReader.getMap(variables(lastRequest()), "input");
        assertEquals("inv-123", input.get("id"));
    }
}
