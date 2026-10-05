package io.github.josemodi97.sageactive4j.service;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.exception.SageActive4jApiException;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.GraphQLRequest;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.input.SageInput;
import io.github.josemodi97.sageactive4j.internal.GraphQLDocuments;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Shared plumbing of the domain clients. Not part of the public API. */
abstract class DomainClient {

    private static final Pattern ENUM_LITERAL = Pattern.compile("[A-Z][A-Z0-9_]*");

    final SageActive4jClient client;

    DomainClient(SageActive4jClient client) {
        this.client = client;
    }

    /** For operations Sage Active documents without {@code X-OrganizationId}. */
    SageActive4jClient global() {
        return client.withOrganization(null);
    }

    /** Fails early, with a clear message, for operations that need an organization. */
    SageActive4jClient organization(String operation) {
        if (client.getOrganizationId() == null) {
            throw new IllegalStateException(operation + " needs an organization: configure organizationId(...) "
                    + "or call withOrganization(id) first");
        }
        return client;
    }

    <T> Connection<T> list(SageActive4jClient target, String document, ListOptions options, String fixedWhere,
                           String defaultOrder, Map<String, ListOptions.VariableValue> fixedVars,
                           Function<Map<String, Object>, T> mapper) {
        GraphQLRequest request = GraphQLDocuments.list(document, options, fixedWhere, null, defaultOrder, fixedVars);
        Map<String, Object> data = target.query(request);
        return Connection.fromJson(require(data, document), mapper);
    }

    /** Runs a single-page list of at most 500 and returns the nodes (for small, bounded collections). */
    <T> List<T> listAll(SageActive4jClient target, String document, String fixedWhere,
                        Map<String, ListOptions.VariableValue> fixedVars, Function<Map<String, Object>, T> mapper) {
        List<T> all = new ArrayList<T>();
        ListOptions options = ListOptions.first(ListOptions.MAX_PAGE_SIZE);
        while (true) {
            Connection<T> page = list(target, document, options, fixedWhere, null, fixedVars, mapper);
            all.addAll(page.getNodes());
            String cursor = page.getPageInfo().getEndCursor();
            if (!page.hasNextPage() || cursor == null || cursor.equals(options.getAfter())) {
                return Collections.unmodifiableList(all);
            }
            options = options.after(cursor);
        }
    }

    /** The named field of {@code data}; Sage Active returning {@code null} for it is an error. */
    static Map<String, Object> require(Map<String, Object> data, String field) {
        Map<String, Object> value = JsonReader.getMap(data, field);
        if (value == null) {
            throw new SageActive4jApiException("Sage Active returned no '" + field + "' in its response",
                    SageActive4jApiException.NO_HTTP_STATUS, null);
        }
        return value;
    }

    static <I extends SageInput<I>> I validated(I input, String name) {
        if (input == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        input.validate();
        return input;
    }

    static String requireId(String id, String name) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return id.trim();
    }

    /** Guards enum values that get inlined into a filter literal. */
    static String enumLiteral(String value, String name) {
        if (value == null || !ENUM_LITERAL.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must be an enum constant like SALES_INVOICE, got: " + value);
        }
        return value;
    }
}
