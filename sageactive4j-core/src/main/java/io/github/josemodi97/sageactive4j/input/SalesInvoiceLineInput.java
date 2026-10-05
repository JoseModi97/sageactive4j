package io.github.josemodi97.sageactive4j.input;

import java.math.BigDecimal;

/**
 * A sales invoice line. Sage Active does <em>not</em> take the price from
 * the product: without {@link #unitPrice(BigDecimal)} the line is priced 0.
 */
public final class SalesInvoiceLineInput extends SageInput<SalesInvoiceLineInput> {

    public SalesInvoiceLineInput productId(String productId) { return set("productId", productId); }
    public SalesInvoiceLineInput totalQuantity(BigDecimal totalQuantity) { return set("totalQuantity", totalQuantity); }
    public SalesInvoiceLineInput totalQuantity(long totalQuantity) { return totalQuantity(BigDecimal.valueOf(totalQuantity)); }
    public SalesInvoiceLineInput unitPrice(BigDecimal unitPrice) { return set("unitPrice", unitPrice); }
    /** Discount percentage. */
    public SalesInvoiceLineInput firstDiscount(BigDecimal firstDiscount) { return set("firstDiscount", firstDiscount); }
    /** Overrides the product name on the line (max 2500). */
    public SalesInvoiceLineInput productName(String productName) { return set("productName", productName); }

    @Override
    public void validate() {
        requireFields("productId", "totalQuantity");
    }
}
