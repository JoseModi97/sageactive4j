package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.model.SalesQuote;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Lists sales quotes. */
@Command(name = "quote", mixinStandardHelpOptions = true,
        description = "List sales quotes in the current organization.")
final class QuoteCommand extends CliCommand implements Callable<Integer> {

    @Option(names = "--first", description = "How many to show (default: ${DEFAULT-VALUE})", defaultValue = "20")
    int first;

    @Override
    public Integer call() {
        try (SageActive4jClient client = root().client()) {
            Connection<SalesQuote> quotes = client.sales().quotes(ListOptions.first(first));
            if (quotes.isEmpty()) {
                out().println("No sales quotes found.");
                return 0;
            }
            Table table = new Table("NUMBER", "DATE", "CUSTOMER", "STATUS", "TOTAL NET");
            for (SalesQuote q : quotes) {
                table.row(q.getOperationalNumber() == null ? "(draft)" : q.getOperationalNumber(),
                        q.getDocumentDate() != null ? q.getDocumentDate() : "",
                        q.getSocialName() != null ? q.getSocialName() : (q.getCustomerId() != null ? q.getCustomerId() : ""),
                        q.getStatus() != null ? q.getStatus() : "",
                        q.getTotalNet() == null ? "" : q.getTotalNet().toPlainString());
            }
            table.print(out());
            if (quotes.getTotalCount() != null && quotes.getTotalCount() > quotes.size()) {
                out().println(quotes.size() + " of " + quotes.getTotalCount() + " shown (--first N for more)");
            }
        }
        return 0;
    }
}
