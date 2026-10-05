package io.github.josemodi97.sageactive4j.input;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A payment applied to one or more open items of a customer's or a
 * supplier's invoices. Sage Active books it as a bank entry on the given
 * payment method (bank account or cash).
 *
 * <pre>{@code
 * new OpenItemSettlementInput()
 *     .entryDate(LocalDate.now())
 *     .paymentMethodId(bankAccountId)
 *     .thirdPartyId(customerId)
 *     .pay(openItem.getId(), openItem.getOutstandingAmount());
 * }</pre>
 */
public final class OpenItemSettlementInput extends SageInput<OpenItemSettlementInput> {

    private final List<Map<String, Object>> payments = new ArrayList<Map<String, Object>>();

    public OpenItemSettlementInput entryDate(LocalDate entryDate) { return set("entryDate", entryDate); }
    /** The bank account or cash register receiving/paying (a {@code PaymentMethod} id). */
    public OpenItemSettlementInput paymentMethodId(String paymentMethodId) { return set("paymentMethodId", paymentMethodId); }
    /** The customer (sales) or supplier (purchases). */
    public OpenItemSettlementInput thirdPartyId(String thirdPartyId) { return set("thirdPartyId", thirdPartyId); }
    public OpenItemSettlementInput description(String description) { return set("description", description); }
    public OpenItemSettlementInput documentNumber(String documentNumber) { return set("documentNumber", documentNumber); }

    /** Pays {@code amount} (full or partial) against one open item. */
    public OpenItemSettlementInput pay(String openItemId, BigDecimal amount) {
        if (openItemId == null || openItemId.trim().isEmpty()) {
            throw new IllegalArgumentException("openItemId must not be blank");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, got " + amount);
        }
        Map<String, Object> payment = new LinkedHashMap<String, Object>();
        payment.put("openItemId", openItemId);
        payment.put("paidAmount", amount);
        payments.add(payment);
        return this;
    }

    public List<Map<String, Object>> getPayments() {
        return Collections.unmodifiableList(payments);
    }

    @Override
    public void validate() {
        requireFields("entryDate", "paymentMethodId", "thirdPartyId");
        if (payments.isEmpty()) {
            throw new IllegalArgumentException("OpenItemSettlementInput needs at least one pay(openItemId, amount)");
        }
    }

    /**
     * The input as sent: the payments go under
     * {@code salesOpenItemLinkagePaidAmounts} or
     * {@code purchaseOpenItemLinkagePaidAmounts} depending on the side.
     */
    public Map<String, Object> toMap(String paymentsField) {
        Map<String, Object> map = new LinkedHashMap<String, Object>(toMap());
        map.put(paymentsField, new ArrayList<Map<String, Object>>(payments));
        return map;
    }
}
