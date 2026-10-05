package io.github.josemodi97.sageactive4j.model;

/** The outcome of {@code sales().createAndPostInvoice(...)}: create, close (number), then post. */
public final class InvoicePosting {

    private final String invoiceId;
    private final String operationalNumber;
    private final AccountingPosting posting;

    public InvoicePosting(String invoiceId, String operationalNumber, AccountingPosting posting) {
        this.invoiceId = invoiceId;
        this.operationalNumber = operationalNumber;
        this.posting = posting;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    /** The invoice number assigned on closing. */
    public String getOperationalNumber() {
        return operationalNumber;
    }

    public AccountingPosting getPosting() {
        return posting;
    }

    @Override
    public String toString() {
        return "InvoicePosting{invoiceId=" + invoiceId + ", operationalNumber=" + operationalNumber
                + ", accountingEntryNumber=" + (posting == null ? null : posting.getAccountingEntryNumber()) + '}';
    }
}
