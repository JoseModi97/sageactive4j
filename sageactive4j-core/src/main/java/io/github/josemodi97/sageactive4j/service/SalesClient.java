package io.github.josemodi97.sageactive4j.service;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.exception.InvoiceWorkflowException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jException;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.graphql.Pages;
import io.github.josemodi97.sageactive4j.input.OpenItemSettlementInput;
import io.github.josemodi97.sageactive4j.input.SalesInvoiceInput;
import io.github.josemodi97.sageactive4j.internal.GraphQLDocuments;
import io.github.josemodi97.sageactive4j.model.AccountingPosting;
import io.github.josemodi97.sageactive4j.model.InvoicePosting;
import io.github.josemodi97.sageactive4j.model.OpenItem;
import io.github.josemodi97.sageactive4j.model.SalesInvoice;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sales invoices through their whole life: create (draft) → close
 * (numbered, locked) → post (ledger entry) → settle open items (payments).
 */
public final class SalesClient extends DomainClient {

    private static final int MAX_DESCRIPTION = 200;

    private final AccountingClient accounting;

    public SalesClient(SageActive4jClient client) {
        super(client);
        this.accounting = new AccountingClient(client);
    }

    /** Invoices with their lines, most recent first. */
    public Connection<SalesInvoice> invoices(ListOptions options) {
        return list(organization("salesInvoices"), "salesInvoices", options, null, "[{ documentDate: DESC }]", null,
                SalesInvoice::new);
    }

    public Iterable<SalesInvoice> allInvoices(ListOptions options) {
        return Pages.iterate(options, this::invoices);
    }

    /** Creates a draft invoice and returns its id. */
    public String createInvoice(SalesInvoiceInput invoice) {
        Map<String, Object> data = organization("createSalesInvoice").query(GraphQLDocuments.operation(
                "createSalesInvoice", Collections.singletonMap("values", validated(invoice, "invoice"))));
        return new SalesInvoice(require(data, "createSalesInvoice")).getId();
    }

    /**
     * Closes (locks and numbers) a draft. The returned invoice carries the
     * {@code id} and assigned {@code operationalNumber}.
     */
    public SalesInvoice closeInvoice(String invoiceId) {
        Map<String, Object> data = organization("closeSalesInvoice").query(GraphQLDocuments.operation(
                "closeSalesInvoice", input("salesInvoiceId", requireId(invoiceId, "invoiceId"))));
        return new SalesInvoice(require(data, "closeSalesInvoice"));
    }

    /**
     * Posts a closed invoice to the ledger.
     *
     * @param journalTypeId a {@code SALES_INVOICE} journal (see {@code accounting().defaultJournalType(...)})
     * @param description   the ledger entry description (max 200)
     */
    public AccountingPosting postInvoice(String invoiceId, String journalTypeId, String description) {
        Map<String, Object> input = new LinkedHashMap<String, Object>();
        input.put("salesInvoiceId", requireId(invoiceId, "invoiceId"));
        input.put("accountingEntryDescription", truncate(requireId(description, "description")));
        input.put("journalTypeId", requireId(journalTypeId, "journalTypeId"));
        Map<String, Object> data = organization("postSalesInvoice").query(GraphQLDocuments.operation(
                "postSalesInvoice", Collections.singletonMap("input", input)));
        return new AccountingPosting(require(data, "postSalesInvoice"));
    }

    /** Creates, closes and posts in one go, on the organization's first active sales journal. */
    public InvoicePosting createAndPostInvoice(SalesInvoiceInput invoice) {
        return createAndPostInvoice(invoice, null, null);
    }

    /**
     * Creates, closes and posts an invoice. <strong>Not atomic</strong>:
     * if closing or posting fails, the invoice still exists and an
     * {@link InvoiceWorkflowException} says which step to resume.
     *
     * @param journalTypeId {@code null} = the first active {@code SALES_INVOICE} journal,
     *                      resolved <em>before</em> anything is created
     * @param description   {@code null} = "Sales invoice &lt;number&gt;"
     */
    public InvoicePosting createAndPostInvoice(SalesInvoiceInput invoice, String journalTypeId, String description) {
        validated(invoice, "invoice");
        String journal = journalTypeId != null ? journalTypeId
                : accounting.defaultJournalType("SALES_INVOICE").getId();

        String invoiceId = createInvoice(invoice);
        String number = null;
        try {
            number = closeInvoice(invoiceId).getOperationalNumber();
        } catch (SageActive4jException e) {
            throw new InvoiceWorkflowException(invoiceId, null, InvoiceWorkflowException.Step.CLOSE, e);
        }
        try {
            String entryDescription = description != null ? description
                    : "Sales invoice " + (number != null ? number : invoiceId);
            return new InvoicePosting(invoiceId, number, postInvoice(invoiceId, journal, entryDescription));
        } catch (SageActive4jException e) {
            throw new InvoiceWorkflowException(invoiceId, number, InvoiceWorkflowException.Step.POST, e);
        }
    }

    /** The installments of an invoice, by due date. */
    public List<OpenItem> openItems(String invoiceId) {
        return listAll(organization("salesInvoiceOpenItems"), "salesInvoiceOpenItems",
                "{ salesInvoiceId: { eq: $invoiceId } }",
                GraphQLDocuments.var("invoiceId", "UUID", requireId(invoiceId, "invoiceId")), OpenItem::new);
    }

    /** Records a customer payment against open items; returns the bank entry created. */
    public AccountingPosting settleOpenItems(OpenItemSettlementInput settlement) {
        validated(settlement, "settlement");
        Map<String, Object> data = organization("salesOpenItemSettlement").query(GraphQLDocuments.operation(
                "salesOpenItemSettlement",
                Collections.singletonMap("input", settlement.toMap("salesOpenItemLinkagePaidAmounts"))));
        return new AccountingPosting(require(data, "salesOpenItemSettlement"));
    }

    static Map<String, Object> input(String field, Object value) {
        return Collections.<String, Object>singletonMap("input", Collections.singletonMap(field, value));
    }

    static String truncate(String description) {
        return description.length() <= MAX_DESCRIPTION ? description : description.substring(0, MAX_DESCRIPTION);
    }
}
