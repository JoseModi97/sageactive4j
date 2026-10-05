package io.github.josemodi97.sageactive4j.model;

import java.util.Map;

/** A ledger account of the chart of accounts. */
public final class AccountingAccount extends SageObject {

    public AccountingAccount(Map<String, Object> json) {
        super(json);
    }

    /**
     * The account number. Accepts both the {@code code { value }} object
     * shape (which the SDK's query selects) and a plain string.
     */
    public String getCode() {
        String nested = string("code", "value");
        return nested != null ? nested : string("code");
    }

    public String getName() { return string("name"); }
    /** Computed: code + name. */
    public String getDescription() { return string("description"); }
    public Boolean getDeactivated() { return bool("deactivated"); }
    /** {@code ACCOUNT}, {@code SUB_ACCOUNT}, {@code GROUP}, ... Only {@code SUB_ACCOUNT}s take entries. */
    public String getAccountLevel() { return string("accountLevel"); }
    public String getSubAccountType() { return string("subAccountType"); }
    public String getAccountType() { return string("accountType"); }
    public Boolean getApplyVat() { return bool("applyVat"); }
    public String getTaxTreatmentId() { return string("taxTreatmentId"); }
}
