package io.github.josemodi97.sageactive4j.input;

import java.math.BigDecimal;

/** A sales quote line. */
public final class SalesQuoteLineInput extends SageInput<SalesQuoteLineInput> {

    public SalesQuoteLineInput productId(String productId) { return set("productId", productId); }
    public SalesQuoteLineInput totalQuantity(BigDecimal totalQuantity) { return set("totalQuantity", totalQuantity); }
    public SalesQuoteLineInput totalQuantity(long totalQuantity) { return totalQuantity(BigDecimal.valueOf(totalQuantity)); }
    public SalesQuoteLineInput unitPrice(BigDecimal unitPrice) { return set("unitPrice", unitPrice); }
    /** Discount percentage. */
    public SalesQuoteLineInput firstDiscount(BigDecimal firstDiscount) { return set("firstDiscount", firstDiscount); }
    /** Overrides the product name on the line. */
    public SalesQuoteLineInput productName(String productName) { return set("productName", productName); }

    @Override
    public void validate() {
        requireFields("productId", "totalQuantity");
    }
}
