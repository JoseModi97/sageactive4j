package io.github.josemodi97.sageactive4j.exception;

/**
 * A multi-step invoice workflow ({@code createAndPostInvoice}) failed after
 * the invoice was created. The invoice exists: {@link #getInvoiceId()} and
 * {@link #getFailedStep()} say where to resume - retry the step, or clean up
 * (a {@link Step#CLOSE} failure leaves a deletable draft; after closing,
 * only a credit note can neutralise it).
 */
public class InvoiceWorkflowException extends SageActive4jException {

    private static final long serialVersionUID = 1L;

    /** The step that failed. */
    public enum Step {
        /** The draft exists, unnumbered. */
        CLOSE,
        /** The invoice is closed and numbered, but not posted to the ledger. */
        POST
    }

    private final String invoiceId;
    private final String operationalNumber;
    private final Step failedStep;

    public InvoiceWorkflowException(String invoiceId, String operationalNumber, Step failedStep, RuntimeException cause) {
        super("Invoice " + invoiceId + (operationalNumber == null ? "" : " (" + operationalNumber + ")")
                + " was created but the " + failedStep + " step failed: " + cause.getMessage(), cause);
        this.invoiceId = invoiceId;
        this.operationalNumber = operationalNumber;
        this.failedStep = failedStep;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    /** The number assigned on closing, or {@code null} if closing failed. */
    public String getOperationalNumber() {
        return operationalNumber;
    }

    public Step getFailedStep() {
        return failedStep;
    }
}
