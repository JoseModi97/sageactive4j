package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.model.Product;
import io.github.josemodi97.sageactive4j.model.SalesTariff;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Lists products or tariffs. */
@Command(name = "product", mixinStandardHelpOptions = true,
        description = "List products or tariffs ('product', 'product tariffs').",
        subcommands = ProductCommand.Tariffs.class)
final class ProductCommand extends CliCommand implements Callable<Integer> {

    @Option(names = "--first", description = "How many to show (default: ${DEFAULT-VALUE})", defaultValue = "20")
    int first;

    @Override
    public Integer call() {
        try (SageActive4jClient client = root().client()) {
            Connection<Product> products = client.products().list(ListOptions.first(first));
            if (products.isEmpty()) {
                out().println("No products found.");
                return 0;
            }
            Table table = new Table("CODE", "NAME", "CATEGORY", "UNIT PRICE", "VAT %");
            for (Product p : products) {
                table.row(p.getCode() != null ? p.getCode() : "",
                        p.getName() != null ? p.getName() : "",
                        p.getCategory() != null ? p.getCategory() : "",
                        p.getSalesUnitPrice() != null ? p.getSalesUnitPrice().toPlainString() : "",
                        p.getSalesVatPercentage() != null ? p.getSalesVatPercentage().toPlainString() + "%" : "");
            }
            table.print(out());
            if (products.getTotalCount() != null && products.getTotalCount() > products.size()) {
                out().println(products.size() + " of " + products.getTotalCount() + " shown (--first N for more)");
            }
        }
        return 0;
    }

    /** Lists tariffs. */
    @Command(name = "tariffs", mixinStandardHelpOptions = true, description = "List configured sales tariffs.")
    static final class Tariffs extends CliCommand implements Callable<Integer> {
        @Option(names = "--first", description = "How many to show (default: ${DEFAULT-VALUE})", defaultValue = "20")
        int first;

        @Override
        public Integer call() {
            try (SageActive4jClient client = root().client()) {
                Connection<SalesTariff> tariffs = client.products().tariffs(ListOptions.first(first));
                if (tariffs.isEmpty()) {
                    out().println("No tariffs found.");
                    return 0;
                }
                Table table = new Table("CODE", "NAME", "TYPE", "ENABLED", "START", "END");
                for (SalesTariff t : tariffs) {
                    table.row(t.getCode() != null ? t.getCode() : "",
                            t.getName() != null ? t.getName() : "",
                            t.getType() != null ? t.getType() : "",
                            Boolean.TRUE.equals(t.isEnabled()) ? "yes" : "no",
                            t.getStartDate() != null ? t.getStartDate() : "",
                            t.getEndDate() != null ? t.getEndDate() : "");
                }
                table.print(out());
            }
            return 0;
        }
    }
}
