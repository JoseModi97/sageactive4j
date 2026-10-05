package io.github.josemodi97.sageactive4j.graphql;

import io.github.josemodi97.sageactive4j.internal.JsonSerializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A GraphQL operation: the document, its variables, and (when the document
 * holds several operations) which one to run. Immutable.
 *
 * <p>Variable values may be anything JSON-representable: {@code String},
 * {@code Boolean}, numbers ({@code BigDecimal} for money), {@code Map},
 * {@code List}, enums, {@code java.time} values (ISO-8601), and the SDK's own
 * input types.
 */
public final class GraphQLRequest implements JsonSerializable {

    private final String query;
    private final Map<String, Object> variables;
    private final String operationName;

    public GraphQLRequest(String query) {
        this(query, null, null);
    }

    public GraphQLRequest(String query, Map<String, ?> variables) {
        this(query, variables, null);
    }

    public GraphQLRequest(String query, Map<String, ?> variables, String operationName) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("GraphQL query must not be blank");
        }
        this.query = query;
        this.variables = variables == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(variables));
        this.operationName = operationName;
    }

    public String getQuery() {
        return query;
    }

    /** Never {@code null}; empty when the operation takes no variables. */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /** May be {@code null}. */
    public String getOperationName() {
        return operationName;
    }

    @Override
    public Object toJsonValue() {
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("query", query);
        if (!variables.isEmpty()) {
            json.put("variables", variables);
        }
        if (operationName != null) {
            json.put("operationName", operationName);
        }
        return json;
    }
}
