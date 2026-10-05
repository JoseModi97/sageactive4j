package io.github.josemodi97.sageactive4j.internal;

import io.github.josemodi97.sageactive4j.graphql.GraphQLRequest;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Loads the SDK's GraphQL documents ({@code queries/*.graphql} resources)
 * and renders list queries: paging variables, the caller's (or the
 * operation's default) {@code where}/{@code order} literals, and any extra
 * variable declarations. Not part of the public API.
 */
public final class GraphQLDocuments {

    private static final String RESOURCE_DIR = "/io/github/josemodi97/sageactive4j/queries/";
    private static final ConcurrentMap<String, String> CACHE = new ConcurrentHashMap<String, String>();

    private GraphQLDocuments() {
    }

    /** The raw document text. @throws IllegalStateException if the resource is missing (a packaging bug) */
    public static String load(String name) {
        String cached = CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        String path = RESOURCE_DIR + name + ".graphql";
        try (InputStream in = GraphQLDocuments.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing GraphQL document " + path + " - the sageactive4j jar is incomplete");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            String text = new String(out.toByteArray(), StandardCharsets.UTF_8);
            CACHE.putIfAbsent(name, text);
            return text;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read GraphQL document " + path, e);
        }
    }

    /** A non-list operation with the given variables. */
    public static GraphQLRequest operation(String name, Map<String, ?> variables) {
        return new GraphQLRequest(load(name), variables);
    }

    /**
     * Renders a list query.
     *
     * @param options      the caller's paging/filter/sort options
     * @param fixedWhere   a filter the operation always applies (e.g. "this invoice's open items"),
     *                     combined with the caller's filter by {@code and}; or {@code null}
     * @param defaultWhere used when the caller gives no filter; or {@code null}
     * @param defaultOrder used when the caller gives no order; or {@code null}
     * @param fixedVars    variables referenced by {@code fixedWhere}: name → {type, value}
     */
    public static GraphQLRequest list(String name, ListOptions options, String fixedWhere, String defaultWhere,
                                      String defaultOrder, Map<String, ListOptions.VariableValue> fixedVars) {
        ListOptions o = options == null ? ListOptions.defaults() : options;

        String where = o.getWhere() != null ? o.getWhere() : defaultWhere;
        if (fixedWhere != null) {
            where = where == null ? fixedWhere : combine(fixedWhere, where);
        }
        String order = o.getOrder() != null ? o.getOrder() : defaultOrder;

        StringBuilder args = new StringBuilder("first: $first, after: $after");
        if (where != null) {
            args.append(", where: ").append(where);
        }
        if (order != null) {
            args.append(", order: ").append(order);
        }

        Map<String, ListOptions.VariableValue> declared = new LinkedHashMap<String, ListOptions.VariableValue>();
        if (fixedVars != null) {
            declared.putAll(fixedVars);
        }
        for (Map.Entry<String, ListOptions.VariableValue> entry : o.getVariables().entrySet()) {
            if (declared.containsKey(entry.getKey())) {
                throw new IllegalArgumentException("Variable $" + entry.getKey() + " is already used by this operation; pick another name");
            }
            declared.put(entry.getKey(), entry.getValue());
        }

        StringBuilder vars = new StringBuilder();
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("first", o.getFirst());
        values.put("after", o.getAfter());
        for (Map.Entry<String, ListOptions.VariableValue> entry : declared.entrySet()) {
            vars.append(", $").append(entry.getKey()).append(": ").append(entry.getValue().getType());
            values.put(entry.getKey(), entry.getValue().getValue());
        }

        String document = load(name);
        if (!document.contains("{{args}}") || !document.contains("{{vars}}")) {
            throw new IllegalStateException("GraphQL document " + name + " is not a list template");
        }
        return new GraphQLRequest(document.replace("{{vars}}", vars).replace("{{args}}", args), values);
    }

    /**
     * ANDs two filter literals. Two object literals are merged flat
     * ({@code { a } + { b } -> { a, b }}): some Sage Active queries
     * (e.g. {@code files}) reject nested {@code and: [...]} groups, while
     * fields at the same level are always ANDed. Anything else falls back
     * to {@code and}.
     */
    static String combine(String fixedWhere, String where) {
        String a = fixedWhere.trim();
        String b = where.trim();
        if (a.startsWith("{") && a.endsWith("}") && b.startsWith("{") && b.endsWith("}")) {
            String innerA = a.substring(1, a.length() - 1).trim();
            String innerB = b.substring(1, b.length() - 1).trim();
            if (innerA.isEmpty()) {
                return b;
            }
            if (innerB.isEmpty()) {
                return a;
            }
            return "{ " + innerA + ", " + innerB + " }";
        }
        return "{ and: [" + a + ", " + b + "] }";
    }

    /** Convenience for one fixed variable. */
    public static Map<String, ListOptions.VariableValue> var(String name, String type, Object value) {
        ListOptions holder = ListOptions.defaults().variable(name, type, value);
        return Collections.unmodifiableMap(new LinkedHashMap<String, ListOptions.VariableValue>(holder.getVariables()));
    }
}
