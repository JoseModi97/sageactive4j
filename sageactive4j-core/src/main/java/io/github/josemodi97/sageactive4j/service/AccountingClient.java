package io.github.josemodi97.sageactive4j.service;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.exception.SageActive4jApiException;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.graphql.Pages;
import io.github.josemodi97.sageactive4j.input.AccountingEntryInput;
import io.github.josemodi97.sageactive4j.internal.GraphQLDocuments;
import io.github.josemodi97.sageactive4j.model.AccountingAccount;
import io.github.josemodi97.sageactive4j.model.AccountingEntry;
import io.github.josemodi97.sageactive4j.model.AccountingExercise;
import io.github.josemodi97.sageactive4j.model.CreatedRecord;
import io.github.josemodi97.sageactive4j.model.JournalType;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Fiscal years, chart of accounts, journals and journal entries. */
public final class AccountingClient extends DomainClient {

    public AccountingClient(SageActive4jClient client) {
        super(client);
    }

    /** Fiscal years, most recent first. */
    public Connection<AccountingExercise> exercises(ListOptions options) {
        return list(organization("accountingExercises"), "accountingExercises", options, null,
                "[{ startDate: DESC }]", null, AccountingExercise::new);
    }

    /**
     * The chart of accounts. To sort, note that an account code is an
     * object: {@code order("[{ code: { value: ASC } }]")}.
     */
    public Connection<AccountingAccount> accounts(ListOptions options) {
        return list(organization("accountingAccounts"), "accountingAccounts", options, null, null, null,
                AccountingAccount::new);
    }

    public Iterable<AccountingAccount> allAccounts(ListOptions options) {
        return Pages.iterate(options, this::accounts);
    }

    public Connection<JournalType> journalTypes(ListOptions options) {
        return list(organization("journalTypes"), "journalTypes", options, null, null, null, JournalType::new);
    }

    /** Active journals of one type, e.g. {@code SALES_INVOICE}, {@code PURCHASE_INVOICE}, {@code FINANCIAL}. */
    public List<JournalType> activeJournalTypes(String type) {
        String where = "{ type: { eq: " + enumLiteral(type, "type") + " }, deactivated: { eq: false } }";
        return listAll(organization("journalTypes"), "journalTypes", where, null, JournalType::new);
    }

    /** The first active journal of a type; fails if the organization has none. */
    public JournalType defaultJournalType(String type) {
        List<JournalType> journals = activeJournalTypes(type);
        if (journals.isEmpty()) {
            throw new SageActive4jApiException("Organization " + client.getOrganizationId()
                    + " has no active " + type + " journal", SageActive4jApiException.NO_HTTP_STATUS, null);
        }
        return journals.get(0);
    }

    /** Journal entries with their lines, most recent first. */
    public Connection<AccountingEntry> entries(ListOptions options) {
        return list(organization("accountingEntries"), "accountingEntries", options, null,
                "[{ date: DESC }]", null, AccountingEntry::new);
    }

    public Iterable<AccountingEntry> allEntries(ListOptions options) {
        return Pages.iterate(options, this::entries);
    }

    /**
     * Creates a balanced journal entry from business codes
     * ({@code createAccountingEntryUsingCodes}). The balance is checked
     * locally first. Returns the entry id and number.
     */
    public CreatedRecord createEntry(AccountingEntryInput entry) {
        Map<String, Object> data = organization("createAccountingEntryUsingCodes").query(GraphQLDocuments.operation(
                "createAccountingEntryUsingCodes",
                Collections.singletonMap("values", validated(entry, "entry"))));
        return new CreatedRecord(require(data, "createAccountingEntryUsingCodes"));
    }
}
