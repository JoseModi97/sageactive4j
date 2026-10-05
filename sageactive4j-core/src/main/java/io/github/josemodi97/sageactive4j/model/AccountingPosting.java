package io.github.josemodi97.sageactive4j.model;

import java.util.List;
import java.util.Map;

/**
 * The accounting entry created by posting an invoice or settling open items.
 * An invoice that deducts an earlier down payment produces two entries:
 * {@link #getAccountingEntryId()} is the first, {@link #getEntries()} lists all.
 */
public final class AccountingPosting extends SageObject {

    public AccountingPosting(Map<String, Object> json) {
        super(json);
    }

    public String getAccountingEntryId() { return string("accountingEntryId"); }
    public Long getAccountingEntryNumber() { return longValue("accountingEntryNumber"); }

    /** All entries created (only selected for sales invoice posting); empty otherwise. */
    public List<AccountingPosting> getEntries() { return list(AccountingPosting::new, "accountingEntries"); }
}
