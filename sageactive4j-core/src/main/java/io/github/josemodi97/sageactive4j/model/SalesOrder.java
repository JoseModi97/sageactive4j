package io.github.josemodi97.sageactive4j.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** A sales order. */
public final class SalesOrder extends SageObject {

    public SalesOrder(Map<String, Object> json) {
        super(json);
    }

    public String getOperationalNumber() { return string("operationalNumber"); }
    public LocalDate getDocumentDate() { return date("documentDate"); }
    public OffsetDateTime getCreationDate() { return dateTime("creationDate"); }
    public String getCustomerId() { return string("customerId"); }
    public String getSocialName() { return string("socialName"); }
    /** E.g. {@code Pending}, {@code Closed}. */
    public String getStatus() { return string("status"); }
    public BigDecimal getTotalNet() { return decimal("totalNet"); }
    public BigDecimal getTotalGross() { return decimal("totalGross"); }
    public BigDecimal getTotalVat() { return decimal("totalVat"); }
    public BigDecimal getTotalLiquid() { return decimal("totalLiquid"); }
    public BigDecimal getDiscount() { return decimal("discount"); }
    public String getDocumentTypeId() { return string("documentTypeId"); }
    public List<Line> getLines() { return list(Line::new, "lines"); }

    /** One line of a {@link SalesOrder}. */
    public static final class Line extends SageObject {
        public Line(Map<String, Object> json) {
            super(json);
        }

        public Integer getOrder() { return integer("order"); }
        public String getProductId() { return string("productId"); }
        public String getProductCode() { return string("productCode"); }
        public String getProductName() { return string("productName"); }
        public BigDecimal getTotalQuantity() { return decimal("totalQuantity"); }
        public BigDecimal getPendingQuantity() { return decimal("pendingQuantity"); }
        public BigDecimal getUnitPrice() { return decimal("unitPrice"); }
        public BigDecimal getFirstDiscount() { return decimal("firstDiscount"); }
        public BigDecimal getTotalNet() { return decimal("totalNet"); }
        public BigDecimal getVatPercentage() { return decimal("vatPercentage"); }
    }
}
