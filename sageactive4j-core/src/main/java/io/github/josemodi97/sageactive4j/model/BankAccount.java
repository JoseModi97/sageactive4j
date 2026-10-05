package io.github.josemodi97.sageactive4j.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * A bank account: the payment method configured in Sage Active (same id as
 * its {@link PaymentMethod}) plus balances and sync state from the bank.
 */
public final class BankAccount extends SageObject {

    public BankAccount(Map<String, Object> json) {
        super(json);
    }

    public String getReferenceName() { return string("referenceName"); }
    /** {@code BANK_ACCOUNT}, {@code CASH} or {@code NOT_SPECIFIED}. */
    public String getType() { return string("type"); }
    public Boolean getDisabled() { return bool("disabled"); }
    public String getIban() { return string("iban"); }
    public String getBic() { return string("bic"); }
    public String getBankName() { return string("bankName"); }
    public String getAccountingAccountCode() { return string("accountingAccountCode"); }
    public String getJournalTypeId() { return string("journalTypeId"); }
    public String getSubAccountId() { return string("subAccountId"); }
    public BigDecimal getBalanceAmount() { return decimal("balanceAmount"); }
    public BigDecimal getBankingAvailableBalanceAmount() { return decimal("bankingAvailableBalanceAmount"); }
    public BigDecimal getBankingLedgerBalanceAmount() { return decimal("bankingLedgerBalanceAmount"); }
    public LocalDate getBankingLastSyncDate() { return date("bankingLastSyncDate"); }
    public Integer getBankingPendingTransactions() { return integer("bankingPendingTransactions"); }
}
