package io.github.josemodi97.sageactive4j.graphql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A raw GraphQL response: {@code data} as nested maps and lists (see
 * {@code JsonReader} for the type mapping), plus any {@code errors} and
 * {@code extensions}. Returned as-is by
 * {@code SageActive4jClient.execute(...)}, which does not throw for GraphQL
 * errors so callers can inspect partial data.
 */
public final class GraphQLResponse {

    private final int httpStatus;
    private final Map<String, Object> data;
    private final List<GraphQLError> errors;
    private final Map<String, Object> extensions;
    private final String rawBody;

    public GraphQLResponse(int httpStatus, Map<String, Object> data, List<GraphQLError> errors,
                           Map<String, Object> extensions, String rawBody) {
        this.httpStatus = httpStatus;
        this.data = data;
        this.errors = errors == null
                ? Collections.<GraphQLError>emptyList()
                : Collections.unmodifiableList(new ArrayList<GraphQLError>(errors));
        this.extensions = extensions == null ? Collections.<String, Object>emptyMap() : extensions;
        this.rawBody = rawBody;
    }

    /**
     * Builds a response from a parsed JSON body. Returns {@code null} if the
     * body has neither {@code data} nor {@code errors} (i.e. isn't a GraphQL
     * response at all).
     */
    @SuppressWarnings("unchecked")
    public static GraphQLResponse fromJson(int httpStatus, Map<String, Object> body, String rawBody) {
        if (body == null || (!body.containsKey("data") && !body.containsKey("errors"))) {
            return null;
        }
        Object data = body.get("data");
        Object errors = body.get("errors");
        Object extensions = body.get("extensions");
        List<GraphQLError> parsedErrors = new ArrayList<GraphQLError>();
        if (errors instanceof List) {
            for (Object error : (List<Object>) errors) {
                parsedErrors.add(GraphQLError.fromJson(error));
            }
        }
        return new GraphQLResponse(httpStatus,
                data instanceof Map ? (Map<String, Object>) data : null,
                parsedErrors,
                extensions instanceof Map ? (Map<String, Object>) extensions : null,
                rawBody);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /** The {@code data} object, or {@code null}. */
    public Map<String, Object> getData() {
        return data;
    }

    /** Never {@code null}; empty on success. */
    public List<GraphQLError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    /** The response body exactly as received. */
    public String getRawBody() {
        return rawBody;
    }
}
