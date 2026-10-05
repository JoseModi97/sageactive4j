package io.github.josemodi97.sageactive4j.input;

import java.time.LocalDate;

/**
 * A new sales invoice ({@code SalesInvoiceCreateGLDtoInput}). Requires a
 * customer and at least one line.
 *
 * <pre>{@code
 * new SalesInvoiceInput()
 *     .customerId(customerId)
 *     .documentDate(LocalDate.now())
 *     .addLine(new SalesInvoiceLineInput().productId(productId).totalQuantity(2).unitPrice(new BigDecimal("150.00")));
 * }</pre>
 *
 * <p>Setting {@link #operationalNumber(String)} (importing an invoice
 * numbered elsewhere) creates it directly in Closed status.
 */
public final class SalesInvoiceInput extends SageInput<SalesInvoiceInput> {

    public SalesInvoiceInput customerId(String customerId) { return set("customerId", customerId); }
    public SalesInvoiceInput documentDate(LocalDate documentDate) { return set("documentDate", documentDate); }
    /** Fulfillment date. */
    public SalesInvoiceInput operationDate(LocalDate operationDate) { return set("operationDate", operationDate); }
    public SalesInvoiceInput operationalNumber(String operationalNumber) { return set("operationalNumber", operationalNumber); }
    /** Shown to the customer (max 1000). */
    public SalesInvoiceInput comments(String comments) { return set("comments", comments); }
    /** Internal notes (max 1000). */
    public SalesInvoiceInput remarks(String remarks) { return set("remarks", remarks); }
    /** The customer's reference (max 20). */
    public SalesInvoiceInput externalReference(String externalReference) { return set("externalReference", externalReference); }
    /** Free text printed on the invoice; not copied from the customer automatically (max 1000). */
    public SalesInvoiceInput specialMention(String specialMention) { return set("specialMention", specialMention); }

    /** Lines keep the order they are added in. */
    public SalesInvoiceInput addLine(SalesInvoiceLineInput line) {
        line.validate();
        return add("lines", line);
    }

    @Override
    public void validate() {
        requireFields("customerId", "lines");
    }
}
