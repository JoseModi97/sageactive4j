package io.github.josemodi97.sageactive4j.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** A supplier invoice. */
public final class PurchaseInvoice extends SageObject {

    public PurchaseInvoice(Map<String, Object> json) {
        super(json);
    }

    /** The supplier's invoice number. */
    public String getInvoiceNumber() { return string("invoiceNumber"); }
    public LocalDate getInvoiceDate() { return date("invoiceDate"); }
    public LocalDate getOperationDate() { return date("operationDate"); }
    public LocalDate getFirstDueDate() { return date("firstDueDate"); }
    /** {@code Pending}, {@code Closed}, {@code Posted}, {@code PartiallyPaid}, {@code Paid}, {@code Uploading}, ... */
    public String getStatus() { return string("status"); }
    /** Details on the status (a business error code when OCR processing failed). */
    public String getDescription() { return string("description"); }
    public String getSupplierId() { return string("supplierId"); }
    public Supplier getSupplier() { return object(Supplier::new, "supplier"); }
    public BigDecimal getTotalLiquid() { return decimal("totalLiquid"); }
    public BigDecimal getPendingAmount() { return decimal("pendingAmount"); }
    public Boolean getHasCashVat() { return bool("hasCashVat"); }
    /** The file the invoice was read from, when created by OCR. */
    public String getFileName() { return string("fileName"); }
    public String getFileId() { return string("fileId"); }
    public OffsetDateTime getCreationDate() { return dateTime("creationDate"); }
    public List<VatLine> getVatLines() { return list(VatLine::new, "vatLines"); }

    /** A VAT breakdown line of a {@link PurchaseInvoice}. */
    public static final class VatLine extends SageObject {
        public VatLine(Map<String, Object> json) {
            super(json);
        }

        public BigDecimal getTotalVat() { return decimal("totalVat"); }
        public BigDecimal getTotalVatBase() { return decimal("totalVatBase"); }
        public String getTaxId() { return string("taxId"); }
        public String getTaxTreatmentId() { return string("taxTreatmentId"); }
        public String getPurchaseAccountingAccountId() { return string("purchaseAccountingAccountId"); }
    }
}
