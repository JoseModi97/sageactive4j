package io.github.josemodi97.sageactive4j.cli;

import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Base of every subcommand: access to the root command's settings and to prompting. */
@Command
abstract class CliCommand {

    @Spec
    CommandSpec spec;

    SageActive4jCli root() {
        return (SageActive4jCli) spec.root().userObject();
    }

    PrintStream out() {
        return root().context.out;
    }

    /** Asks a question; returns the answer, {@code defaultValue} for an empty answer or end of input. */
    String prompt(String question, String defaultValue) {
        CliContext context = root().context;
        context.out.print(question + (defaultValue == null || defaultValue.isEmpty() ? "" : " [" + defaultValue + "]") + ": ");
        context.out.flush();
        try {
            String line = context.in.readLine();
            if (line == null || line.trim().isEmpty()) {
                context.out.println();
                return defaultValue;
            }
            return line.trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Like {@link #prompt} but without echo when a real terminal is attached. */
    String promptSecret(String question) {
        CliContext context = root().context;
        Console console = System.console();
        if (context.interactive && console != null) {
            char[] secret = console.readPassword("%s: ", question);
            return secret == null || secret.length == 0 ? null : new String(secret).trim();
        }
        return prompt(question, null);
    }

    boolean confirm(String question, boolean defaultYes) {
        String answer = prompt(question + (defaultYes ? " (Y/n)" : " (y/N)"), null);
        if (answer == null) {
            return defaultYes;
        }
        return answer.toLowerCase(java.util.Locale.ROOT).startsWith("y");
    }
}
