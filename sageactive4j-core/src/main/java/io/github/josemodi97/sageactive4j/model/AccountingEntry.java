package io.github.josemodi97.sageactive4j.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** A balanced journal entry. */
public final class AccountingEntry extends SageObject {

    public AccountingEntry(Map<String, Object> json) {
        super(json);
    }

    public Long getNumber() { return longValue("number"); }
    public LocalDate getDate() { return date("date"); }
    public LocalDate getDocumentDate() { return date("documentDate"); }
    public String getDocumentNumber() { return string("documentNumber"); }
    public String getDescription() { return string("description"); }
    public Boolean getIsClosed() { return bool("isClosed"); }
    public String getEntryType() { return string("entryType"); }
    public String getJournalTypeId() { return string("journalTypeId"); }
    public JournalType getJournalType() { return object(JournalType::new, "journalType"); }
    public List<AccountingEntryLine> getLines() { return list(AccountingEntryLine::new, "accountingEntryLines"); }
}
