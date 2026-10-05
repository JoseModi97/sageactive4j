package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.GraphQLError;
import io.github.josemodi97.sageactive4j.graphql.GraphQLRequest;
import io.github.josemodi97.sageactive4j.graphql.GraphQLResponse;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import io.github.josemodi97.sageactive4j.internal.JsonWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** Runs any GraphQL query or mutation and pretty-prints the result. */
@Command(name = "query", description = {"Run a GraphQL query or mutation and print the JSON result.",
    "Examples:", "  sageactive4j query \"{ userProfile { fullName } }\"",
    "  sageactive4j query -f customers.graphql --var first=10 --var code=\\\"C001\\\""})
final class QueryCommand extends CliCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", paramLabel = "GRAPHQL", description = "The query text")
    String query;

    @Option(names = {"-f", "--file"}, description = "Read the query from a .graphql file")
    Path file;

    @Option(names = "--var", paramLabel = "NAME=VALUE",
            description = "A variable; VALUE is parsed as JSON when it is valid JSON (10, true, \"text\", {...}), else taken as a string")
    Map<String, String> vars = new LinkedHashMap<String, String>();

    @Option(names = "--raw", description = "Print the whole response (data and errors) instead of just data")
    boolean raw;

    @Option(names = "--no-org", description = "Send no X-OrganizationId (for userProfile, organizations, ...)")
    boolean noOrg;

    @Override
    public Integer call() {
        if ((query == null) == (file == null)) {
            throw new CliException("Give the query either as an argument or with -f FILE (exactly one).");
        }
        String text;
        try {
            text = file != null ? new String(Files.readAllBytes(file), StandardCharsets.UTF_8) : query;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
        Map<String, Object> variables = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, String> var : vars.entrySet()) {
            variables.put(var.getKey(), parseValue(var.getValue()));
        }

        try (SageActive4jClient base = root().client()) {
            SageActive4jClient client = noOrg ? base.withOrganization(null) : base;
            GraphQLResponse response = client.execute(new GraphQLRequest(text, variables));
            if (raw) {
                out().println(JsonWriter.writePretty(JsonReader.parse(response.getRawBody())));
            } else if (response.getData() != null) {
                out().println(JsonWriter.writePretty(response.getData()));
            }
            if (response.hasErrors()) {
                for (GraphQLError error : response.getErrors()) {
                    root().context.err.println("GraphQL error: " + error);
                }
                return 1;
            }
            return 0;
        }
    }

    static Object parseValue(String value) {
        try {
            return JsonReader.parse(value);
        } catch (RuntimeException notJson) {
            return value;
        }
    }
}
