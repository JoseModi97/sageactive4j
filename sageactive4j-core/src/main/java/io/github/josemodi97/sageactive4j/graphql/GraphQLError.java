package io.github.josemodi97.sageactive4j.graphql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One entry of a GraphQL response's {@code errors} array. Immutable. */
public final class GraphQLError {

    private final String message;
    private final List<Object> path;
    private final Map<String, Object> extensions;

    public GraphQLError(String message, List<Object> path, Map<String, Object> extensions) {
        this.message = message == null ? "" : message;
        this.path = path == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<Object>(path));
        this.extensions = extensions == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(extensions));
    }

    @SuppressWarnings("unchecked")
    static GraphQLError fromJson(Object json) {
        if (!(json instanceof Map)) {
            return new GraphQLError(String.valueOf(json), null, null);
        }
        Map<String, Object> map = (Map<String, Object>) json;
        Object message = map.get("message");
        Object path = map.get("path");
        Object extensions = map.get("extensions");
        return new GraphQLError(
                message == null ? null : message.toString(),
                path instanceof List ? (List<Object>) path : null,
                extensions instanceof Map ? (Map<String, Object>) extensions : null);
    }

    public String getMessage() {
        return message;
    }

    /** Path to the failing field (names and list indexes); empty if not field-specific. */
    public List<Object> getPath() {
        return path;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    /**
     * {@code extensions.code}, the machine-readable error code; resolve it
     * to a localized message with Sage Active's {@code localizedErrorMessage}
     * query. {@code null} if absent.
     */
    public String getCode() {
        Object code = extensions.get("code");
        return code == null ? null : code.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(message);
        if (getCode() != null) {
            sb.append(" [").append(getCode()).append(']');
        }
        if (!path.isEmpty()) {
            sb.append(" at ").append(path);
        }
        return sb.toString();
    }
}
