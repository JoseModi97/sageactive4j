package io.github.josemodi97.sageactive4j.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** A transaction imported from a connected bank, with its reconciliation state. */
public final class BankMovement extends SageObject {

    public BankMovement(Map<String, Object> json) {
        super(json);
    }

    public String getBankAccountId() { return string("bankAccountId"); }
    public BigDecimal getTransactionAmount() { return decimal("transactionAmount"); }
    public LocalDate getDatePosted() { return date("datePosted"); }
    public LocalDate getDateFundsAvailable() { return date("dateFundsAvailable"); }
    public String getTransactionNarrative() { return string("transactionNarrative"); }
    public String getNarrative1() { return string("narrative1"); }
    public String getReferenceNumber() { return string("referenceNumber"); }
    public String getTransactionType() { return string("transactionType"); }
    public String getTransactionStatus() { return string("transactionStatus"); }
    /** {@code PENDING}, {@code IN_PROGRESS}, {@code LINKED}, {@code LINKED_WITH_VARIANCE}, {@code DISMISSED}, {@code NOT_SPECIFIED}. */
    public String getLinkStatus() { return string("linkStatus"); }
    public LocalDate getLinkStatusDate() { return date("linkStatusDate"); }
    public List<LinkedEntry> getLinkedAccountingEntries() { return list(LinkedEntry::new, "linkedAccountingEntries"); }

    public boolean isReconciled() {
        String status = getLinkStatus();
        return "LINKED".equals(status) || "LINKED_WITH_VARIANCE".equals(status);
    }

    /** A bank-journal entry linked to a {@link BankMovement}. */
    public static final class LinkedEntry extends SageObject {
        public LinkedEntry(Map<String, Object> json) {
            super(json);
        }

        public String getAccountingEntryId() { return string("accountingEntryId"); }
        public LocalDate getDate() { return date("date"); }
        public Long getNumber() { return longValue("number"); }
        public String getDocumentNumber() { return string("documentNumber"); }
        public String getDescription() { return string("description"); }
        public String getStatus() { return string("status"); }
        public Boolean getIsClosed() { return bool("isClosed"); }
    }
}
