package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.model.Tax;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Lists tax rates and definitions. */
@Command(name = "tax", mixinStandardHelpOptions = true,
        description = "List tax rates and definitions in the current organization.")
final class TaxCommand extends CliCommand implements Callable<Integer> {

    @Option(names = "--first", description = "How many to show (default: ${DEFAULT-VALUE})", defaultValue = "50")
    int first;

    @Override
    public Integer call() {
        try (SageActive4jClient client = root().client()) {
            Connection<Tax> taxes = client.accounting().taxes(ListOptions.first(first));
            if (taxes.isEmpty()) {
                out().println("No taxes found.");
                return 0;
            }
            Table table = new Table("ID", "NAME", "PERCENTAGE", "TYPE", "ACTIVE");
            for (Tax t : taxes) {
                table.row(t.getId() != null ? t.getId() : "",
                        t.getName() != null ? t.getName() : "",
                        t.getPercentage() != null ? t.getPercentage().toPlainString() + "%" : "",
                        t.getTaxType() != null ? t.getTaxType() : "",
                        Boolean.TRUE.equals(t.isInactive()) ? "no" : "yes");
            }
            table.print(out());
            if (taxes.getTotalCount() != null && taxes.getTotalCount() > taxes.size()) {
                out().println(taxes.size() + " of " + taxes.getTotalCount() + " shown (--first N for more)");
            }
        }
        return 0;
    }
}
