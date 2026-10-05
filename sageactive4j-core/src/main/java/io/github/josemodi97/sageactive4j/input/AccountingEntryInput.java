package io.github.josemodi97.sageactive4j.input;

import io.github.josemodi97.sageactive4j.internal.JsonReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A journal entry built from business codes
 * ({@code AccountingEntryCreateUsingCodesGLDtoInput}): journal code, account
 * codes and third-party codes rather than Sage Active ids.
 *
 * <pre>{@code
 * new AccountingEntryInput()
 *     .journalTypeCode("VTE").date(LocalDate.now()).documentNumber("FA0022")
 *     .addLine(AccountingEntryLineInput.debit("411000000", new BigDecimal("158.25")).thirdCode("BAGUES"))
 *     .addLine(AccountingEntryLineInput.credit("701005000", new BigDecimal("150.00")))
 *     .addLine(AccountingEntryLineInput.credit("445710500", new BigDecimal("8.25")));
 * }</pre>
 */
public final class AccountingEntryInput extends SageInput<AccountingEntryInput> {

    public AccountingEntryInput journalTypeCode(String journalTypeCode) { return set("journalTypeCode", journalTypeCode); }
    public AccountingEntryInput date(LocalDate date) { return set("date", date); }
    /** Defaults to {@link #date(LocalDate)}. */
    public AccountingEntryInput documentDate(LocalDate documentDate) { return set("documentDate", documentDate); }
    /** Mandatory in DE. */
    public AccountingEntryInput documentNumber(String documentNumber) { return set("documentNumber", documentNumber); }
    /** Mandatory in DE; copied to lines without their own description. */
    public AccountingEntryInput description(String description) { return set("description", description); }

    public AccountingEntryInput addLine(AccountingEntryLineInput line) {
        line.validate();
        return add("accountingEntryLines", line);
    }

    /** Also checks the entry balances (total debit = total credit), so an obvious mistake fails before the call. */
    @Override
    public void validate() {
        requireFields("journalTypeCode", "date", "accountingEntryLines");
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (Object item : (List<?>) get("accountingEntryLines")) {
            AccountingEntryLineInput line = (AccountingEntryLineInput) item;
            debit = debit.add(amount(line.get("debitAmount")));
            credit = credit.add(amount(line.get("creditAmount")));
        }
        if (debit.compareTo(credit) != 0) {
            throw new IllegalArgumentException("Accounting entry is unbalanced: debit " + debit.toPlainString()
                    + " != credit " + credit.toPlainString());
        }
    }

    private static BigDecimal amount(Object value) {
        BigDecimal amount = JsonReader.toBigDecimal(value);
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
