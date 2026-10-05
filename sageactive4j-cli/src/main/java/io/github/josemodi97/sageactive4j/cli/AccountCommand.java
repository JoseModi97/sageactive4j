package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.model.AccountingAccount;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Lists ledger accounts from the chart of accounts. */
@Command(name = "account", description = "List ledger accounts from the chart of accounts.")
final class AccountCommand extends CliCommand implements Callable<Integer> {

    @Option(names = "--first", description = "How many to show (default: ${DEFAULT-VALUE})", defaultValue = "20")
    int first;

    @Override
    public Integer call() {
        try (SageActive4jClient client = root().client()) {
            Connection<AccountingAccount> accounts = client.accounting().accounts(ListOptions.first(first));
            if (accounts.isEmpty()) {
                out().println("No accounting accounts found.");
                return 0;
            }
            Table table = new Table("CODE", "NAME", "LEVEL", "TYPE", "ACTIVE");
            for (AccountingAccount a : accounts) {
                table.row(a.getCode() != null ? a.getCode() : "",
                        a.getName() != null ? a.getName() : "",
                        a.getAccountLevel() != null ? a.getAccountLevel() : "",
                        a.getAccountType() != null ? a.getAccountType() : "",
                        Boolean.TRUE.equals(a.getDeactivated()) ? "no" : "yes");
            }
            table.print(out());
            if (accounts.getTotalCount() != null && accounts.getTotalCount() > accounts.size()) {
                out().println(accounts.size() + " of " + accounts.getTotalCount() + " shown (--first N for more)");
            }
        }
        return 0;
    }
}
