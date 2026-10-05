package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.model.SalesOrder;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Lists sales orders. */
@Command(name = "order", description = "List sales orders in the current organization.")
final class OrderCommand extends CliCommand implements Callable<Integer> {

    @Option(names = "--first", description = "How many to show (default: ${DEFAULT-VALUE})", defaultValue = "20")
    int first;

    @Override
    public Integer call() {
        try (SageActive4jClient client = root().client()) {
            Connection<SalesOrder> orders = client.sales().orders(ListOptions.first(first));
            if (orders.isEmpty()) {
                out().println("No sales orders found.");
                return 0;
            }
            Table table = new Table("NUMBER", "DATE", "CUSTOMER", "STATUS", "TOTAL");
            for (SalesOrder o : orders) {
                table.row(o.getOperationalNumber() == null ? "(draft)" : o.getOperationalNumber(),
                        o.getDocumentDate() != null ? o.getDocumentDate() : "",
                        o.getSocialName() != null ? o.getSocialName() : (o.getCustomerId() != null ? o.getCustomerId() : ""),
                        o.getStatus() != null ? o.getStatus() : "",
                        o.getTotalLiquid() == null ? "" : o.getTotalLiquid().toPlainString());
            }
            table.print(out());
            if (orders.getTotalCount() != null && orders.getTotalCount() > orders.size()) {
                out().println(orders.size() + " of " + orders.getTotalCount() + " shown (--first N for more)");
            }
        }
        return 0;
    }
}
