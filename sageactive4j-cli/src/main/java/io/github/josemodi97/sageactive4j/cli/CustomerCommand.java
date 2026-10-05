package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.model.Customer;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Lists customers. */
@Command(name = "customer", mixinStandardHelpOptions = true,
        description = "List customers in the current organization.")
final class CustomerCommand extends CliCommand implements Callable<Integer> {

    @Option(names = "--first", description = "How many to show (default: ${DEFAULT-VALUE})", defaultValue = "20")
    int first;

    @Override
    public Integer call() {
        try (SageActive4jClient client = root().client()) {
            Connection<Customer> customers = client.thirdParties().customers(ListOptions.first(first));
            if (customers.isEmpty()) {
                out().println("No customers found.");
                return 0;
            }
            Table table = new Table("CODE", "NAME", "COUNTRY", "VAT NUMBER", "STATUS");
            for (Customer c : customers) {
                table.row(c.getCode() != null ? c.getCode() : "",
                        c.getSocialName() != null ? c.getSocialName() : (c.getTradeName() != null ? c.getTradeName() : ""),
                        c.getCountryAcronym() != null ? c.getCountryAcronym() : "",
                        c.getVatNumber() != null ? c.getVatNumber() : "",
                        c.getStatus() != null ? c.getStatus() : "");
            }
            table.print(out());
            if (customers.getTotalCount() != null && customers.getTotalCount() > customers.size()) {
                out().println(customers.size() + " of " + customers.getTotalCount() + " shown (--first N for more)");
            }
        }
        return 0;
    }
}
