package io.github.josemodi97.sageactive4j.input;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Context for computing a product price ({@code ProductPriceGLDtoInput}).
 * All fields are optional; tariff and discount group default to the
 * customer's sales terms.
 */
public final class ProductPriceRequest extends SageInput<ProductPriceRequest> {

    public ProductPriceRequest customerId(String customerId) { return set("customerId", customerId); }
    /** Overrides the customer's tariff. */
    public ProductPriceRequest salesTariffId(String salesTariffId) { return set("salesTariffId", salesTariffId); }
    /** Overrides the customer's discount group. */
    public ProductPriceRequest salesDiscountGroupId(String salesDiscountGroupId) { return set("salesDiscountGroupId", salesDiscountGroupId); }
    public ProductPriceRequest documentDate(LocalDate documentDate) { return set("documentDate", documentDate); }
    public ProductPriceRequest quantity(BigDecimal quantity) { return set("quantity", quantity); }
    /** Only for a lead without a customer; never together with {@code customerId}. */
    public ProductPriceRequest documentTypeId(String documentTypeId) { return set("documentTypeId", documentTypeId); }

    @Override
    public void validate() {
        if (get("customerId") != null && get("documentTypeId") != null) {
            throw new IllegalArgumentException("Pass either customerId or documentTypeId, not both");
        }
    }
}
