package io.github.josemodi97.sageactive4j.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.FileUpload;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.input.OpenItemSettlementInput;
import io.github.josemodi97.sageactive4j.input.ReconcileInput;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import io.github.josemodi97.sageactive4j.model.AccountingPosting;
import io.github.josemodi97.sageactive4j.model.BankAccount;
import io.github.josemodi97.sageactive4j.model.BankMovement;
import io.github.josemodi97.sageactive4j.model.PurchaseInvoice;
import io.github.josemodi97.sageactive4j.testsupport.DomainTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PurchasesAndBanksTest extends DomainTestSupport {

    @Test
    void purchaseInvoicesUseInvoiceNumberAndDate() {
        respond("{\"purchaseInvoices\":{\"nodes\":[{\"id\":\"pi1\",\"invoiceNumber\":\"AA123\",\"invoiceDate\":\"2024-08-19T00:00:00Z\","
                + "\"status\":\"Posted\",\"supplier\":{\"code\":\"70001\",\"socialName\":\"ACME\"},\"totalLiquid\":240,\"pendingAmount\":240,"
                + "\"vatLines\":[{\"totalVat\":40,\"totalVatBase\":200}]}],\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");

        PurchaseInvoice invoice = sage.purchases().invoices(ListOptions.defaults()).getNodes().get(0);

        assertEquals("AA123", invoice.getInvoiceNumber());
        assertEquals(LocalDate.of(2024, 8, 19), invoice.getInvoiceDate());
        assertEquals("ACME", invoice.getSupplier().getSocialName());
        assertEquals(new BigDecimal("40"), invoice.getVatLines().get(0).getTotalVat());
        assertContains(query(lastRequest()), "order: [{ invoiceDate: DESC }]");
    }

    @Test
    void postPurchaseInvoiceMatchesTheDocumentedExample() {
        respond("{\"postPurchaseInvoice\":{\"accountingEntryId\":\"0e1987c3-fc14-4906-8b26-42e4a4c36079\",\"accountingEntryNumber\":819}}");

        AccountingPosting posting = sage.purchases().postInvoice("pi-1", "journal-ach",
                "Purchase Invoice No. 0007 27/06/2024", LocalDate.of(2024, 11, 20));

        assertEquals(819L, posting.getAccountingEntryNumber());
        Map<String, Object> input = JsonReader.getMap(variables(lastRequest()), "input");
        assertEquals("pi-1", input.get("purchaseInvoiceId"));
        assertEquals("journal-ach", input.get("journalTypeId"));
        assertEquals("2024-11-20", input.get("postingDate"));
    }

    @Test
    void postPurchaseInvoiceCanResolveTheJournal() {
        respond("{\"journalTypes\":{\"nodes\":[{\"id\":\"j-ach\",\"type\":\"PURCHASE_INVOICE\"}],\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");
        respond("{\"postPurchaseInvoice\":{\"accountingEntryId\":\"e\",\"accountingEntryNumber\":1}}");
        sage.purchases().postInvoice("pi-1", "Supplier invoice AA123");
        assertEquals("j-ach", JsonReader.getString(variables(request(1)), "input", "journalTypeId"));
        assertFalse(JsonReader.getMap(variables(request(1)), "input").containsKey("postingDate"));
    }

    @Test
    void purchaseSettlementUsesThePurchaseLinkageField() {
        respond("{\"purchaseOpenItemSettlement\":{\"accountingEntryId\":\"0e1987c3-ed48-4906-8b26-42e4a4c36079\",\"accountingEntryNumber\":818}}");
        sage.purchases().settleOpenItems(new OpenItemSettlementInput().entryDate(LocalDate.of(2024, 1, 1))
                .paymentMethodId("pm").thirdPartyId("sup").pay("oi", new BigDecimal("500")));
        Map<String, Object> input = JsonReader.getMap(variables(lastRequest()), "input");
        assertTrue(input.containsKey("purchaseOpenItemLinkagePaidAmounts"));
        assertFalse(input.containsKey("salesOpenItemLinkagePaidAmounts"));
    }

    @Test
    void purchaseOpenItems() {
        respond("{\"purchaseInvoiceOpenItems\":{\"nodes\":[{\"id\":\"poi\",\"amount\":240,\"paidAmountAccumulated\":240,"
                + "\"status\":\"PAID\",\"purchaseInvoiceId\":\"pi-1\"}],\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");
        assertTrue(sage.purchases().openItems("pi-1").get(0).isSettled());
        assertContains(query(lastRequest()), "where: { purchaseInvoiceId: { eq: $invoiceId } }");
    }

    @Test
    void bankAccountsAcceptAPlainList() {
        respond("{\"bankAccounts\":[{\"id\":\"b1\",\"referenceName\":\"Main\",\"iban\":\"FR7630006000011234567890189\","
                + "\"balanceAmount\":1520.35,\"bankingLastSyncDate\":\"2026-10-04T00:00:00Z\"}]}");
        List<BankAccount> accounts = sage.banks().bankAccounts();
        assertEquals("Main", accounts.get(0).getReferenceName());
        assertEquals(new BigDecimal("1520.35"), accounts.get(0).getBalanceAmount());
        assertEquals(LocalDate.of(2026, 10, 4), accounts.get(0).getBankingLastSyncDate());
    }

    @Test
    void bankAccountsAlsoAcceptAConnection() {
        respond("{\"bankAccounts\":{\"nodes\":[{\"id\":\"b1\",\"referenceName\":\"Main\"}]}}");
        assertEquals("Main", sage.banks().bankAccounts().get(0).getReferenceName());
    }

    @Test
    void movementsFilterOnTheBankAccount() {
        respond("{\"bankMovements\":{\"nodes\":[{\"id\":\"m1\",\"bankAccountId\":\"b1\",\"transactionAmount\":-42.50,"
                + "\"datePosted\":\"2026-10-03T00:00:00Z\",\"transactionNarrative\":\"CARD SHOP\",\"linkStatus\":\"LINKED\","
                + "\"linkedAccountingEntries\":[{\"accountingEntryId\":\"ae9\",\"number\":12}]}],\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");

        BankMovement movement = sage.banks().movements("b1", ListOptions.defaults()).getNodes().get(0);

        assertEquals(new BigDecimal("-42.50"), movement.getTransactionAmount());
        assertTrue(movement.isReconciled());
        assertEquals("ae9", movement.getLinkedAccountingEntries().get(0).getAccountingEntryId());
        assertContains(query(lastRequest()), "where: { bankAccountId: { eq: $bankAccountId } }");
        assertEquals("b1", variables(lastRequest()).get("bankAccountId"));
    }

    @Test
    void reconcileMatchesTheDocumentedExample() {
        respond("{\"reconcileBankMovement\":{\"id\":\"11111111-1111-1111-1111-111111111111\"}}");

        String id = sage.banks().reconcile(new ReconcileInput()
                .bankTransactionId("11111111-1111-1111-1111-111111111111")
                .addAccountingEntry("22222222-2222-2222-2222-222222222222")
                .addAccountingEntry("33333333-3333-3333-3333-333333333333"));

        assertEquals("11111111-1111-1111-1111-111111111111", id);
        Map<String, Object> input = JsonReader.getMap(variables(lastRequest()), "input");
        assertEquals("11111111-1111-1111-1111-111111111111", input.get("bankTransactionId"));
        @SuppressWarnings("unchecked")
        Map<String, Object> second = (Map<String, Object>) JsonReader.getList(input, "accountingEntries").get(1);
        assertEquals("33333333-3333-3333-3333-333333333333", second.get("accountingEntryId"));
    }

    @Test
    void reconcileNeedsAnEntry() {
        assertThrows(IllegalArgumentException.class,
                () -> sage.banks().reconcile(new ReconcileInput().bankTransactionId("t")));
    }

    @Test
    void unreconcileUsesTheCapitalRMutationName() {
        respond("{\"unReconcileBankMovement\":{\"id\":\"t1\"}}");
        assertEquals("t1", sage.banks().unreconcile("t1"));
        assertContains(query(lastRequest()), "unReconcileBankMovement(input: $input)");
        assertEquals("t1", JsonReader.getString(variables(lastRequest()), "input", "bankTransactionId"));
    }

    @Test
    void paymentMethods() {
        respond("{\"paymentMethods\":{\"nodes\":[{\"id\":\"pm1\",\"referenceName\":\"Cash\",\"type\":\"CASH\"}],"
                + "\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");
        assertEquals("CASH", sage.banks().paymentMethods(ListOptions.defaults()).getNodes().get(0).getType());
    }

    @Test
    void uploadReceiptForOcrAndQueryByFileId() {
        respond("{\"uploadFileToEntity\":{\"id\":\"receipt-file-123.pdf\"}}");
        String fileId = sage.purchases().uploadReceipt(FileUpload.of("receipt.pdf", "application/pdf", new byte[] { 1, 2, 3 }));
        assertEquals("receipt-file-123.pdf", fileId);

        respond("{\"purchaseInvoices\":{\"nodes\":[{\"id\":\"pi-ocr-1\",\"invoiceNumber\":\"INV-OCR-01\","
                + "\"status\":\"Pending\",\"fileId\":\"receipt-file-123.pdf\",\"fileName\":\"receipt.pdf\"}],"
                + "\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");
        Connection<PurchaseInvoice> invoices = sage.purchases().invoicesByFileId("receipt-file-123.pdf", ListOptions.defaults());
        assertEquals(1L, invoices.getTotalCount());
        PurchaseInvoice invoice = invoices.getNodes().get(0);
        assertEquals("INV-OCR-01", invoice.getInvoiceNumber());
        assertEquals("receipt-file-123.pdf", invoice.getFileId());
        assertEquals("receipt.pdf", invoice.getFileName());
        assertContains(query(lastRequest()), "where: { fileId: { eq: $fileId } }");
    }
}
