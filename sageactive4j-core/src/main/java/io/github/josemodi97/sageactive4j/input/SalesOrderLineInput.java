package io.github.josemodi97.sageactive4j.input;

import java.math.BigDecimal;

/** A sales order line. */
public final class SalesOrderLineInput extends SageInput<SalesOrderLineInput> {

    public SalesOrderLineInput productId(String productId) { return set("productId", productId); }
    public SalesOrderLineInput totalQuantity(BigDecimal totalQuantity) { return set("totalQuantity", totalQuantity); }
    public SalesOrderLineInput totalQuantity(long totalQuantity) { return totalQuantity(BigDecimal.valueOf(totalQuantity)); }
    public SalesOrderLineInput unitPrice(BigDecimal unitPrice) { return set("unitPrice", unitPrice); }
    /** Discount percentage. */
    public SalesOrderLineInput firstDiscount(BigDecimal firstDiscount) { return set("firstDiscount", firstDiscount); }
    /** Overrides the product name on the line. */
    public SalesOrderLineInput productName(String productName) { return set("productName", productName); }

    @Override
    public void validate() {
        requireFields("productId", "totalQuantity");
    }
}
