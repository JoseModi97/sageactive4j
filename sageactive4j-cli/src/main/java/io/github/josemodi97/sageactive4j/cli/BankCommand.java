package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.model.BankAccount;
import io.github.josemodi97.sageactive4j.model.BankMovement;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Lists bank accounts or bank movements. */
@Command(name = "bank", description = "List bank accounts or movements ('bank accounts', 'bank movements').",
        subcommands = {BankCommand.Accounts.class, BankCommand.Movements.class, BankCommand.Unreconcile.class})
final class BankCommand extends CliCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return new Accounts().call();
    }

    /** Lists bank accounts. */
    @Command(name = "accounts", description = "List connected bank accounts.")
    static final class Accounts extends CliCommand implements Callable<Integer> {
        @Override
        public Integer call() {
            try (SageActive4jClient client = root().client()) {
                List<BankAccount> accounts = client.banks().bankAccounts();
                if (accounts.isEmpty()) {
                    out().println("No bank accounts found.");
                    return 0;
                }
                Table table = new Table("ID", "IBAN", "BANK / LABEL", "BALANCE", "TYPE");
                for (BankAccount b : accounts) {
                    table.row(b.getId() != null ? b.getId() : "",
                            b.getIban() != null ? b.getIban() : "",
                            b.getBankName() != null ? b.getBankName() : (b.getReferenceName() != null ? b.getReferenceName() : ""),
                            b.getBalanceAmount() != null ? b.getBalanceAmount().toPlainString() : "",
                            b.getType() != null ? b.getType() : "");
                }
                table.print(out());
            }
            return 0;
        }
    }

    /** Lists imported bank movements. */
    @Command(name = "movements", description = "List imported bank movements.")
    static final class Movements extends CliCommand implements Callable<Integer> {
        @Option(names = "--account", description = "Filter by bank account id")
        String bankAccountId;

        @Option(names = "--first", description = "How many to show (default: ${DEFAULT-VALUE})", defaultValue = "20")
        int first;

        @Override
        public Integer call() {
            try (SageActive4jClient client = root().client()) {
                Connection<BankMovement> movements = client.banks().movements(bankAccountId, ListOptions.first(first));
                if (movements.isEmpty()) {
                    out().println("No bank movements found.");
                    return 0;
                }
                Table table = new Table("ID", "DATE", "AMOUNT", "NARRATIVE", "STATUS");
                for (BankMovement m : movements) {
                    table.row(m.getId() != null ? m.getId() : "",
                            m.getDatePosted() != null ? m.getDatePosted() : "",
                            m.getTransactionAmount() != null ? m.getTransactionAmount().toPlainString() : "",
                            m.getTransactionNarrative() != null ? m.getTransactionNarrative() : (m.getNarrative1() != null ? m.getNarrative1() : ""),
                            m.getLinkStatus() != null ? m.getLinkStatus() : "");
                }
                table.print(out());
                if (movements.getTotalCount() != null && movements.getTotalCount() > movements.size()) {
                    out().println(movements.size() + " of " + movements.getTotalCount() + " shown (--first N for more)");
                }
            }
            return 0;
        }
    }

    /** Unreconciles a bank movement. */
    @Command(name = "unreconcile", description = "Unreconcile a previously linked bank movement.")
    static final class Unreconcile extends CliCommand implements Callable<Integer> {
        @Option(names = "--movement", required = true, description = "Bank movement / transaction ID")
        String movementId;

        @Override
        public Integer call() {
            try (SageActive4jClient client = root().client()) {
                String resultId = client.banks().unreconcile(movementId);
                out().println("Unreconciled bank movement: " + resultId);
            }
            return 0;
        }
    }
}
