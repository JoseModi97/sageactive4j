package io.github.josemodi97.sageactive4j.model;

import java.math.BigDecimal;
import java.util.Map;

/** A product or service that can be sold. */
public final class Product extends SageObject {

    public Product(Map<String, Object> json) {
        super(json);
    }

    public String getCode() { return string("code"); }
    public String getName() { return string("name"); }
    /** {@code PRODUCT}, {@code SERVICE}, {@code PRODUCT_AND_SERVICE}, {@code FEE}, {@code CONCEPT}, ... */
    public String getCategory() { return string("category"); }
    /** Description shown to the customer on documents. */
    public String getLineDescription() { return string("lineDescription"); }
    public String getComments() { return string("comments"); }
    public Boolean getObsolete() { return bool("obsolete"); }
    /** Base price. Invoice lines do not inherit it automatically: pass it as the line unit price. */
    public BigDecimal getSalesUnitPrice() { return decimal("salesUnitPrice"); }
    public BigDecimal getSalesVatPercentage() { return decimal("salesVatPercentage"); }
    public BigDecimal getFirstSalesDiscount() { return decimal("firstSalesDiscount"); }
    public String getUnitOfMeasurementId() { return string("unitOfMeasurementId"); }
    public String getTaxGroupId() { return string("taxGroupId"); }
    public String getSalesAccountingAccountId() { return string("salesAccountingAccountId"); }
}
