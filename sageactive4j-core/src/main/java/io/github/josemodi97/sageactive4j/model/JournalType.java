package io.github.josemodi97.sageactive4j.model;

import java.util.Map;

/** An accounting journal (sales, purchases, bank, general, ...). */
public final class JournalType extends SageObject {

    public JournalType(Map<String, Object> json) {
        super(json);
    }

    public String getCode() { return string("code"); }
    public String getName() { return string("name"); }
    /** {@code SALES_INVOICE}, {@code PURCHASE_INVOICE}, {@code FINANCIAL}, {@code GENERAL}, {@code CARRY_FORWARD}, {@code CLOSING}. */
    public String getType() { return string("type"); }
    public Boolean getDeactivated() { return bool("deactivated"); }
    public String getAccountingAccountId() { return string("accountingAccountId"); }
}
