package io.github.josemodi97.sageactive4j.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** One installment (due date) of a sales or purchase invoice, and how much of it is paid. */
public final class OpenItem extends SageObject {

    public OpenItem(Map<String, Object> json) {
        super(json);
    }

    public BigDecimal getAmount() { return decimal("amount"); }
    public BigDecimal getPaidAmountAccumulated() { return decimal("paidAmountAccumulated"); }

    /** {@code amount - paidAmountAccumulated}; {@code null} if the amount is unknown. */
    public BigDecimal getOutstandingAmount() {
        BigDecimal amount = getAmount();
        if (amount == null) {
            return null;
        }
        BigDecimal paid = getPaidAmountAccumulated();
        return paid == null ? amount : amount.subtract(paid);
    }

    public LocalDate getDueDate() { return date("dueDate"); }
    /** {@code NOT_SPECIFIED}, {@code PARTIAL}, {@code COLLECTED} (sales) or {@code PAID} (purchases). */
    public String getStatus() { return string("status"); }

    /** Fully settled: {@code COLLECTED} or {@code PAID}. */
    public boolean isSettled() {
        String status = getStatus();
        return "COLLECTED".equals(status) || "PAID".equals(status);
    }

    public Boolean getIsAdvanceInvoicePayment() { return bool("isAdvanceInvoicePayment"); }
    public String getPaymentMeanId() { return string("paymentMeanId"); }

    /** The sales or purchase invoice this item belongs to. */
    public String getInvoiceId() {
        String sales = string("salesInvoiceId");
        return sales != null ? sales : string("purchaseInvoiceId");
    }
}
