package io.github.josemodi97.sageactive4j.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * A product price and discount for a given customer, date and quantity,
 * after Sage Active applied tariff and discount rules.
 */
public final class ProductPrice extends SageObject {

    public ProductPrice(Map<String, Object> json) {
        super(json);
    }

    public Component getPrice() { return object(Component::new, "price"); }
    public Component getFirstDiscount() { return object(Component::new, "firstDiscount"); }

    /** Shortcut for {@code getPrice().getValue()}. */
    public BigDecimal getUnitPrice() {
        Component price = getPrice();
        return price == null ? null : price.getValue();
    }

    /** Shortcut for {@code getFirstDiscount().getValue()} (a percentage). */
    public BigDecimal getDiscountPercentage() {
        Component discount = getFirstDiscount();
        return discount == null ? null : discount.getValue();
    }

    public String getTaxGroupId() { return string("taxGroupId"); }
    public String getTaxId() { return string("taxId"); }
    public BigDecimal getTaxPercentage() { return decimal("taxPercentage"); }
    public String getTaxTreatmentId() { return string("taxTreatmentId"); }
    /** Spain only; always 0 for FR and DE. */
    public BigDecimal getEquivalenceSurchargePercentage() { return decimal("equivalenceSurchargePercentage"); }

    /** A computed price or discount, with the rule it came from. */
    public static final class Component extends SageObject {
        public Component(Map<String, Object> json) {
            super(json);
        }

        public String getCode() { return string("code"); }
        public String getName() { return string("name"); }
        /** Which rule produced the value (product default, tariff, discount group, ...). */
        public String getSource() { return string("source"); }
        public BigDecimal getValue() { return decimal("value"); }
    }
}
