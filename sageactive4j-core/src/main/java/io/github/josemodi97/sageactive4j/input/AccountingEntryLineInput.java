package io.github.josemodi97.sageactive4j.input;

import java.math.BigDecimal;

/** One line of an {@link AccountingEntryInput}. Both amounts are always sent (0 for the unused side). */
public final class AccountingEntryLineInput extends SageInput<AccountingEntryLineInput> {

    public AccountingEntryLineInput() {
        set("debitAmount", BigDecimal.ZERO);
        set("creditAmount", BigDecimal.ZERO);
    }

    public static AccountingEntryLineInput debit(String subAccountCode, BigDecimal amount) {
        return new AccountingEntryLineInput().subAccountCode(subAccountCode).debitAmount(amount);
    }

    public static AccountingEntryLineInput credit(String subAccountCode, BigDecimal amount) {
        return new AccountingEntryLineInput().subAccountCode(subAccountCode).creditAmount(amount);
    }

    /** An existing sub-account code, e.g. {@code 411000000}. */
    public AccountingEntryLineInput subAccountCode(String subAccountCode) { return set("subAccountCode", subAccountCode); }
    /** Customer/supplier/employee code; its type is inferred from the account class. */
    public AccountingEntryLineInput thirdCode(String thirdCode) { return set("thirdCode", thirdCode); }
    public AccountingEntryLineInput description(String description) { return set("description", description); }

    public AccountingEntryLineInput debitAmount(BigDecimal amount) {
        return set("debitAmount", amount == null ? BigDecimal.ZERO : amount);
    }

    public AccountingEntryLineInput creditAmount(BigDecimal amount) {
        return set("creditAmount", amount == null ? BigDecimal.ZERO : amount);
    }

    @Override
    public void validate() {
        requireFields("subAccountCode");
    }
}
