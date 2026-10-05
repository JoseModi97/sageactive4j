package io.github.josemodi97.sageactive4j.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** A sales invoice (or credit note / advance invoice, see {@link #getType()}). */
public final class SalesInvoice extends SageObject {

    public SalesInvoice(Map<String, Object> json) {
        super(json);
    }

    /** The invoice number; {@code null} until the invoice is closed. */
    public String getOperationalNumber() { return string("operationalNumber"); }
    public LocalDate getDocumentDate() { return date("documentDate"); }
    public LocalDate getOperationDate() { return date("operationDate"); }
    public LocalDate getFirstDueDate() { return date("firstDueDate"); }
    /** {@code Pending}, {@code Closed}, {@code Posted}, {@code PartiallyCollected}, {@code Collected}. */
    public String getStatus() { return string("status"); }
    /** {@code NORMAL}, {@code CREDIT_NOTE}, {@code PAYMENT_IN_ADVANCE}, {@code NONE}. */
    public String getType() { return string("type"); }
    public String getSocialName() { return string("socialName"); }
    public String getCustomerId() { return string("customerId"); }
    public Customer getCustomer() { return object(Customer::new, "customer"); }
    public BigDecimal getTotalNet() { return decimal("totalNet"); }
    public BigDecimal getTotalGross() { return decimal("totalGross"); }
    public BigDecimal getTotalDiscount() { return decimal("totalDiscount"); }
    public BigDecimal getTotalVat() { return decimal("totalVat"); }
    /** Amount due, taxes included. */
    public BigDecimal getTotalLiquid() { return decimal("totalLiquid"); }
    public String getComments() { return string("comments"); }
    public String getExternalReference() { return string("externalReference"); }
    public Boolean getHasCreditNote() { return bool("hasCreditNote"); }
    public OffsetDateTime getCreationDate() { return dateTime("creationDate"); }
    public List<Line> getLines() { return list(Line::new, "lines"); }

    /** One line of a {@link SalesInvoice}. */
    public static final class Line extends SageObject {
        public Line(Map<String, Object> json) {
            super(json);
        }

        public Integer getOrder() { return integer("order"); }
        public String getProductId() { return string("productId"); }
        public String getProductCode() { return string("productCode"); }
        public String getProductName() { return string("productName"); }
        public BigDecimal getTotalQuantity() { return decimal("totalQuantity"); }
        public BigDecimal getUnitPrice() { return decimal("unitPrice"); }
        /** Percentage. */
        public BigDecimal getFirstDiscount() { return decimal("firstDiscount"); }
        public BigDecimal getVatPercentage() { return decimal("vatPercentage"); }
        public BigDecimal getTotalNet() { return decimal("totalNet"); }
        public BigDecimal getTotalVat() { return decimal("totalVat"); }
        public BigDecimal getTotalLiquid() { return decimal("totalLiquid"); }
    }
}
