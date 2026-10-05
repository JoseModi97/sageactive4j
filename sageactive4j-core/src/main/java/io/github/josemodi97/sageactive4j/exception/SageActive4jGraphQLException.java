package io.github.josemodi97.sageactive4j.exception;

import io.github.josemodi97.sageactive4j.graphql.GraphQLError;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The response carried a GraphQL {@code errors} array. GraphQL can return
 * partial data alongside errors; it is available from {@link #getData()}.
 */
public class SageActive4jGraphQLException extends SageActive4jApiException {

    private static final long serialVersionUID = 1L;

    private final transient List<GraphQLError> errors;
    private final transient Map<String, Object> data;

    public SageActive4jGraphQLException(String message, int httpStatus, String responseBody,
                                        List<GraphQLError> errors, Map<String, Object> data) {
        super(message, httpStatus, responseBody);
        this.errors = errors == null ? Collections.<GraphQLError>emptyList() : errors;
        this.data = data;
    }

    /** Every error Sage Active reported; never empty. */
    public List<GraphQLError> getErrors() {
        return errors;
    }

    /** The first error's {@code extensions.code}, or {@code null}. */
    public String getErrorCode() {
        return errors.isEmpty() ? null : errors.get(0).getCode();
    }

    /** Partial {@code data}, or {@code null} if none was returned. */
    public Map<String, Object> getData() {
        return data;
    }
}
