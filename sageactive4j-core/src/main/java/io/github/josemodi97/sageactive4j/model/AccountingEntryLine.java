package io.github.josemodi97.sageactive4j.model;

import java.math.BigDecimal;
import java.util.Map;

/** One debit or credit line of an {@link AccountingEntry}. */
public final class AccountingEntryLine extends SageObject {

    public AccountingEntryLine(Map<String, Object> json) {
        super(json);
    }

    public Integer getOrder() { return integer("order"); }
    public String getDescription() { return string("description"); }
    public String getDocumentNumber() { return string("documentNumber"); }
    public BigDecimal getDebitAmount() { return decimal("debitAmount"); }
    public BigDecimal getCreditAmount() { return decimal("creditAmount"); }
    public String getSubAccountId() { return string("subAccountId"); }
    /** The customer, supplier or employee on the line, or {@code null}. */
    public String getThirdPartyId() { return string("accountingEntryThirdParty", "thirdPartyId"); }
    public String getThirdPartyCode() { return string("accountingEntryThirdParty", "code"); }
    public String getThirdPartyName() { return string("accountingEntryThirdParty", "socialName"); }
    /** {@code CUSTOMER}, {@code SUPPLIER} or {@code EMPLOYEE}. */
    public String getThirdPartyOrigin() { return string("accountingEntryThirdParty", "origin"); }
}
