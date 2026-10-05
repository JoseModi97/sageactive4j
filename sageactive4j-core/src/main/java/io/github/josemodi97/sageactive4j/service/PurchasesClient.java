package io.github.josemodi97.sageactive4j.service;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.graphql.Pages;
import io.github.josemodi97.sageactive4j.input.OpenItemSettlementInput;
import io.github.josemodi97.sageactive4j.internal.GraphQLDocuments;
import io.github.josemodi97.sageactive4j.model.AccountingPosting;
import io.github.josemodi97.sageactive4j.model.OpenItem;
import io.github.josemodi97.sageactive4j.model.PurchaseInvoice;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Supplier invoices: list, post to the ledger, settle open items. */
public final class PurchasesClient extends DomainClient {

    private final AccountingClient accounting;

    public PurchasesClient(SageActive4jClient client) {
        super(client);
        this.accounting = new AccountingClient(client);
    }

    /** Supplier invoices, most recent first. */
    public Connection<PurchaseInvoice> invoices(ListOptions options) {
        return list(organization("purchaseInvoices"), "purchaseInvoices", options, null, "[{ invoiceDate: DESC }]",
                null, PurchaseInvoice::new);
    }

    public Iterable<PurchaseInvoice> allInvoices(ListOptions options) {
        return Pages.iterate(options, this::invoices);
    }

    /**
     * Posts a supplier invoice to the ledger.
     *
     * @param journalTypeId a {@code PURCHASE_INVOICE} journal
     * @param description   the ledger entry description (max 200)
     * @param postingDate   accounting date; {@code null} = Sage Active's default
     */
    public AccountingPosting postInvoice(String invoiceId, String journalTypeId, String description,
                                         LocalDate postingDate) {
        Map<String, Object> input = new LinkedHashMap<String, Object>();
        input.put("purchaseInvoiceId", requireId(invoiceId, "invoiceId"));
        input.put("accountingEntryDescription", SalesClient.truncate(requireId(description, "description")));
        input.put("journalTypeId", requireId(journalTypeId, "journalTypeId"));
        if (postingDate != null) {
            input.put("postingDate", postingDate);
        }
        Map<String, Object> data = organization("postPurchaseInvoice").query(GraphQLDocuments.operation(
                "postPurchaseInvoice", Collections.singletonMap("input", input)));
        return new AccountingPosting(require(data, "postPurchaseInvoice"));
    }

    /** Posts on the organization's first active {@code PURCHASE_INVOICE} journal. */
    public AccountingPosting postInvoice(String invoiceId, String description) {
        return postInvoice(invoiceId, accounting.defaultJournalType("PURCHASE_INVOICE").getId(), description, null);
    }

    /** The installments of a supplier invoice. */
    public List<OpenItem> openItems(String invoiceId) {
        return listAll(organization("purchaseInvoiceOpenItems"), "purchaseInvoiceOpenItems",
                "{ purchaseInvoiceId: { eq: $invoiceId } }",
                GraphQLDocuments.var("invoiceId", "UUID", requireId(invoiceId, "invoiceId")), OpenItem::new);
    }

    /** Records a payment to a supplier against open items; returns the bank entry created. */
    public AccountingPosting settleOpenItems(OpenItemSettlementInput settlement) {
        validated(settlement, "settlement");
        Map<String, Object> data = organization("purchaseOpenItemSettlement").query(GraphQLDocuments.operation(
                "purchaseOpenItemSettlement",
                Collections.singletonMap("input", settlement.toMap("purchaseOpenItemLinkagePaidAmounts"))));
        return new AccountingPosting(require(data, "purchaseOpenItemSettlement"));
    }
}
