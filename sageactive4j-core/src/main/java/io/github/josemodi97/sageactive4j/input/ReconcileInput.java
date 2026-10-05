package io.github.josemodi97.sageactive4j.input;

import java.util.Collections;

/** Links a bank movement to one or more accounting entries ({@code ReconcileBankMovementGLDtoInput}). */
public final class ReconcileInput extends SageInput<ReconcileInput> {

    /** The bank movement id. */
    public ReconcileInput bankTransactionId(String bankTransactionId) { return set("bankTransactionId", bankTransactionId); }
    /** Optional rule to attach, e.g. from the movement's proposals. */
    public ReconcileInput bankingRuleId(String bankingRuleId) { return set("bankingRuleId", bankingRuleId); }

    public ReconcileInput addAccountingEntry(String accountingEntryId) {
        if (accountingEntryId == null || accountingEntryId.trim().isEmpty()) {
            throw new IllegalArgumentException("accountingEntryId must not be blank");
        }
        return add("accountingEntries", Collections.singletonMap("accountingEntryId", accountingEntryId));
    }

    @Override
    public void validate() {
        requireFields("bankTransactionId", "accountingEntries");
    }
}
