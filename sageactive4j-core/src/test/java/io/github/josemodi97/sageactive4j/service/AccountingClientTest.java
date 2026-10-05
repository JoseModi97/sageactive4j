package io.github.josemodi97.sageactive4j.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.input.AccountingEntryInput;
import io.github.josemodi97.sageactive4j.input.AccountingEntryLineInput;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import io.github.josemodi97.sageactive4j.model.AccountingAccount;
import io.github.josemodi97.sageactive4j.model.AccountingEntry;
import io.github.josemodi97.sageactive4j.model.AccountingExercise;
import io.github.josemodi97.sageactive4j.model.CreatedRecord;
import io.github.josemodi97.sageactive4j.testsupport.DomainTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AccountingClientTest extends DomainTestSupport {

    @Test
    void exercises() {
        respond("{\"accountingExercises\":{\"nodes\":[{\"id\":\"e1\",\"exercise\":2026,\"description\":\"Exercice 2026\","
                + "\"startDate\":\"2026-01-01T00:00:00Z\",\"endDate\":\"2026-12-31T00:00:00Z\",\"status\":\"OPEN\"}],"
                + "\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");
        AccountingExercise exercise = sage.accounting().exercises(ListOptions.defaults()).getNodes().get(0);
        assertEquals(Integer.valueOf(2026), exercise.getExercise());
        assertEquals(LocalDate.of(2026, 12, 31), exercise.getEndDate());
        assertTrue(exercise.isOpen());
    }

    @Test
    void accountCodeIsReadFromEitherShape() {
        respond("{\"accountingAccounts\":{\"nodes\":[{\"id\":\"a1\",\"code\":{\"value\":\"411000000\"},\"name\":\"Clients\"},"
                + "{\"id\":\"a2\",\"code\":\"706000000\",\"name\":\"Prestations\"}],\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":2}}");
        List<AccountingAccount> accounts = sage.accounting().accounts(ListOptions.defaults()).getNodes();
        assertEquals("411000000", accounts.get(0).getCode());
        assertEquals("706000000", accounts.get(1).getCode());
        assertContains(query(lastRequest()), "code { value }");
    }

    @Test
    void activeJournalsFilterByTypeLiteral() {
        respond("{\"journalTypes\":{\"nodes\":[{\"id\":\"j1\",\"code\":\"ACH\",\"type\":\"PURCHASE_INVOICE\"}],"
                + "\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");
        assertEquals("ACH", sage.accounting().defaultJournalType("PURCHASE_INVOICE").getCode());
        assertContains(query(lastRequest()), "where: { type: { eq: PURCHASE_INVOICE }, deactivated: { eq: false } }");
    }

    @Test
    void journalTypeCannotInjectIntoTheFilter() {
        assertThrows(IllegalArgumentException.class,
                () -> sage.accounting().activeJournalTypes("SALES_INVOICE } }, or: [{ id: { neq: null"));
        assertThrows(IllegalArgumentException.class, () -> sage.accounting().activeJournalTypes("sales"));
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void entriesWithLinesAndThirdParties() {
        respond("{\"accountingEntries\":{\"nodes\":[{\"id\":\"ae1\",\"number\":785,\"date\":\"2023-01-01T00:00:00Z\","
                + "\"documentNumber\":\"FA0022\",\"journalType\":{\"code\":\"VTE\"},\"accountingEntryLines\":["
                + "{\"debitAmount\":158.25,\"creditAmount\":0,\"accountingEntryThirdParty\":{\"code\":\"BAGUES\",\"origin\":\"CUSTOMER\"}},"
                + "{\"debitAmount\":0,\"creditAmount\":158.25}]}],\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");
        AccountingEntry entry = sage.accounting().entries(ListOptions.defaults()).getNodes().get(0);
        assertEquals(785L, entry.getNumber());
        assertEquals("VTE", entry.getJournalType().getCode());
        assertEquals("BAGUES", entry.getLines().get(0).getThirdPartyCode());
        assertEquals("CUSTOMER", entry.getLines().get(0).getThirdPartyOrigin());
        assertEquals(new BigDecimal("158.25"), entry.getLines().get(1).getCreditAmount());
    }

    @Test
    void createEntryMatchesTheDocumentedExample() {
        respond("{\"createAccountingEntryUsingCodes\":{\"id\":\"e451aff7-004d-4e72-a6fb-4ea25e9d9c64\",\"number\":785}}");

        CreatedRecord created = sage.accounting().createEntry(new AccountingEntryInput()
                .description("Facture FA0022")
                .date(LocalDate.of(2023, 1, 1))
                .documentDate(LocalDate.of(2022, 12, 16))
                .documentNumber("FA0022")
                .journalTypeCode("VTE")
                .addLine(AccountingEntryLineInput.debit("411000000", new BigDecimal("158.25")).thirdCode("BAGUES")
                        .description("Facture FA0022 regularisation"))
                .addLine(AccountingEntryLineInput.credit("701005000", new BigDecimal("150")))
                .addLine(AccountingEntryLineInput.credit("445710500", new BigDecimal("8.25"))));

        assertEquals(785L, created.getNumber());
        Map<String, Object> values = JsonReader.getMap(variables(lastRequest()), "values");
        assertEquals("VTE", values.get("journalTypeCode"));
        assertEquals("2023-01-01", values.get("date"));
        List<Object> lines = JsonReader.getList(values, "accountingEntryLines");
        assertEquals(3, lines.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) lines.get(0);
        assertEquals("BAGUES", first.get("thirdCode"));
        assertEquals("158.25", JsonReader.getString(first, "debitAmount"));
        assertEquals(0L, first.get("creditAmount"), "both amounts are always sent");
    }

    @Test
    void unbalancedEntriesNeverLeaveTheMachine() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> sage.accounting().createEntry(new AccountingEntryInput()
                        .journalTypeCode("OD").date(LocalDate.now())
                        .addLine(AccountingEntryLineInput.debit("6262", new BigDecimal("100")))
                        .addLine(AccountingEntryLineInput.credit("512", new BigDecimal("99.99")))));
        assertTrue(e.getMessage().contains("unbalanced"), e.getMessage());
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void numericAmountsSetDirectlyStillBalance() {
        respond("{\"createAccountingEntryUsingCodes\":{\"id\":\"x\",\"number\":1}}");
        sage.accounting().createEntry(new AccountingEntryInput().journalTypeCode("OD").date(LocalDate.now())
                .addLine(new AccountingEntryLineInput().subAccountCode("6262").set("debitAmount", 100))
                .addLine(new AccountingEntryLineInput().subAccountCode("512").set("creditAmount", 100)));
        assertEquals(1, server.requests().size());
    }
}
