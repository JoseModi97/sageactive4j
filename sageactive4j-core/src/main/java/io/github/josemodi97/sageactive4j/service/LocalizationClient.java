package io.github.josemodi97.sageactive4j.service;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.exception.SageActive4jGraphQLException;
import io.github.josemodi97.sageactive4j.graphql.GraphQLError;
import io.github.josemodi97.sageactive4j.internal.GraphQLDocuments;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Human-readable messages for Sage Active's business error keys, which
 * arrive as the GraphQL error message, e.g.
 * {@code sales.businessErrors.invalidDocumentId}.
 */
public final class LocalizationClient extends DomainClient {

    public LocalizationClient(SageActive4jClient client) {
        super(client);
    }

    /**
     * @param language {@code fr}, {@code en}, {@code es}, {@code de}, or a full code such as
     *                 {@code fr-FR} (e.g. {@code UserProfile.getApplicationLanguageCode()})
     */
    public String errorMessage(String errorCode, String language) {
        Map<String, Object> variables = new LinkedHashMap<String, Object>();
        variables.put("errorCode", requireId(errorCode, "errorCode"));
        variables.put("language", requireId(language, "language"));
        Map<String, Object> data = global().query(GraphQLDocuments.operation("localizedErrorMessage", variables));
        return JsonReader.getString(require(data, "localizedErrorMessage"), "message");
    }

    /**
     * Localizes the first error of a GraphQL exception if its message is a
     * Sage Active error key; otherwise returns the message unchanged.
     * Never throws: if localization itself fails, the raw message is returned.
     */
    public String explain(SageActive4jGraphQLException e, String language) {
        if (e.getErrors().isEmpty()) {
            return e.getMessage();
        }
        GraphQLError first = e.getErrors().get(0);
        String key = first.getMessage();
        if (!isErrorKey(key)) {
            return key;
        }
        try {
            String message = errorMessage(key, language);
            return message == null || message.isEmpty() ? key : message;
        } catch (RuntimeException localizationFailed) {
            return key;
        }
    }

    static boolean isErrorKey(String message) {
        return message != null && message.matches("[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*\\.businessErrors\\.[A-Za-z0-9.]+");
    }
}
