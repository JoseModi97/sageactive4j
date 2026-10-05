package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.input.SalesInvoiceInput;
import io.github.josemodi97.sageactive4j.input.SalesInvoiceLineInput;
import io.github.josemodi97.sageactive4j.model.InvoicePosting;
import io.github.josemodi97.sageactive4j.model.SalesInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Lists sales invoices, or creates and posts one. */
@Command(name = "invoice", description = "List recent sales invoices ('invoice create' to create one).",
        subcommands = InvoiceCommand.Create.class)
final class InvoiceCommand extends CliCommand implements Callable<Integer> {

    @Option(names = "--first", description = "How many (default: ${DEFAULT-VALUE})", defaultValue = "20")
    int first;

    @Override
    public Integer call() {
        try (SageActive4jClient client = root().client()) {
            Connection<SalesInvoice> invoices = client.sales().invoices(ListOptions.first(first));
            if (invoices.isEmpty()) {
                out().println("No sales invoices.");
                return 0;
            }
            Table table = new Table("NUMBER", "DATE", "CUSTOMER", "STATUS", "TOTAL");
            for (SalesInvoice invoice : invoices) {
                table.row(invoice.getOperationalNumber() == null ? "(draft)" : invoice.getOperationalNumber(),
                        invoice.getDocumentDate(),
                        invoice.getCustomer() == null ? invoice.getSocialName() : invoice.getCustomer().getSocialName(),
                        invoice.getStatus(),
                        invoice.getTotalLiquid() == null ? "" : invoice.getTotalLiquid().toPlainString());
            }
            table.print(out());
            if (invoices.getTotalCount() != null && invoices.getTotalCount() > invoices.size()) {
                out().println(invoices.size() + " of " + invoices.getTotalCount() + " shown (--first N for more)");
            }
        }
        return 0;
    }

    /**
     * {@code invoice create}: creates, numbers and posts a one-line invoice.
     * Refused unless the profile is marked as a sandbox, or {@code --yes-really}
     * is given - the SDK can't tell a sandbox tenant from a live one.
     */
    @Command(name = "create", description = "Create, number and post a one-line sales invoice.")
    static final class Create extends CliCommand implements Callable<Integer> {

        @Option(names = "--customer", required = true, description = "Customer id")
        String customerId;

        @Option(names = "--product", required = true, description = "Product id")
        String productId;

        @Option(names = "--quantity", description = "Quantity (default: ${DEFAULT-VALUE})", defaultValue = "1")
        BigDecimal quantity;

        @Option(names = "--price", required = true, description = "Unit price (Sage Active does not take it from the product)")
        BigDecimal price;

        @Option(names = "--journal", description = "SALES_INVOICE journal id (default: the first active one)")
        String journalTypeId;

        @Option(names = "--yes-really", description = "Allow this on a profile that is not marked as a sandbox")
        boolean yesReally;

        @Override
        public Integer call() {
            SageActive4jCli root = root();
            ConfigFile file = root.configFile();
            String profile = root.profileName(file);
            boolean sandbox = "true".equalsIgnoreCase(root.setting(file, profile, null, "SAGEACTIVE4J_SANDBOX", "sandbox"));
            if (!sandbox && !yesReally) {
                throw new CliException("Refusing to create a real invoice: profile '" + profile + "' is not marked as a "
                        + "sandbox. Mark it with 'sageactive4j init --profile " + profile + " --sandbox', or pass "
                        + "--yes-really if you mean it.");
            }
            try (SageActive4jClient client = root.client()) {
                InvoicePosting posted = client.sales().createAndPostInvoice(new SalesInvoiceInput()
                        .customerId(customerId)
                        .documentDate(LocalDate.now())
                        .addLine(new SalesInvoiceLineInput().productId(productId).totalQuantity(quantity).unitPrice(price)),
                        journalTypeId, null);
                out().println("Created and posted invoice " + posted.getOperationalNumber() + " (id " + posted.getInvoiceId()
                        + "), ledger entry #" + posted.getPosting().getAccountingEntryNumber());
            }
            return 0;
        }
    }
}
