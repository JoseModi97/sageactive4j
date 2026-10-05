package io.github.josemodi97.sageactive4j.model;

import java.util.Map;

/**
 * A bank account or cash register as configured in Sage Active. Its id is
 * what open-item settlements take as {@code paymentMethodId}.
 */
public final class PaymentMethod extends SageObject {

    public PaymentMethod(Map<String, Object> json) {
        super(json);
    }

    public String getReferenceName() { return string("referenceName"); }
    /** {@code BANK_ACCOUNT}, {@code CASH} or {@code NOT_SPECIFIED}. */
    public String getType() { return string("type"); }
    public Boolean getDisabled() { return bool("disabled"); }
    public String getJournalTypeId() { return string("journalTypeId"); }
    public String getSubAccountId() { return string("subAccountId"); }
}
