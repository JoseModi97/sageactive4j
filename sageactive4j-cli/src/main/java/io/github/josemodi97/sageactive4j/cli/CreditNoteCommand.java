package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Generates a credit note for a posted sales invoice. */
@Command(name = "credit-note", description = "Generate a credit note for a posted sales invoice.")
final class CreditNoteCommand extends CliCommand implements Callable<Integer> {

    @Option(names = "--invoice", required = true, description = "Posted sales invoice id (UUID)")
    String invoiceId;

    @Override
    public Integer call() {
        try (SageActive4jClient client = root().client()) {
            String creditNoteId = client.sales().generateCreditNote(invoiceId);
            out().println("Created credit note with id: " + creditNoteId);
        }
        return 0;
    }
}
