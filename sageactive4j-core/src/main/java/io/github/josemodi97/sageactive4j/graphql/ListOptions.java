package io.github.josemodi97.sageactive4j.graphql;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Paging, filtering and sorting for list queries. Immutable: every method
 * returns a new instance.
 *
 * <p>{@code where} and {@code order} are GraphQL literals, written exactly
 * as in Sage Active's documentation, so any documented filter works without
 * the SDK having to model it:
 *
 * <pre>{@code
 * ListOptions.first(100)
 *     .where("{ socialName: { contains: \"acme\" }, disabled: { eq: false } }")
 *     .order("[{ socialName: ASC }]");
 * }</pre>
 *
 * <p>Literals are inserted into the query text, so never build them from
 * untrusted input; pass such values as variables instead:
 *
 * <pre>{@code
 * ListOptions.first(50)
 *     .where("{ customerId: { eq: $customerId } }")
 *     .variable("customerId", "UUID!", customerId);
 * }</pre>
 */
public final class ListOptions {

    /** Sage Active's maximum page size. */
    public static final int MAX_PAGE_SIZE = 500;
    /** Page size used when none is given. */
    public static final int DEFAULT_PAGE_SIZE = 50;

    private static final Pattern VARIABLE_NAME = Pattern.compile("[_A-Za-z][_0-9A-Za-z]*");

    private final int first;
    private final String after;
    private final String where;
    private final String order;
    private final Map<String, VariableValue> variables;

    private ListOptions(int first, String after, String where, String order, Map<String, VariableValue> variables) {
        this.first = first;
        this.after = after;
        this.where = where;
        this.order = order;
        this.variables = variables;
    }

    /** {@value #DEFAULT_PAGE_SIZE} records, no filter, the operation's default order. */
    public static ListOptions defaults() {
        return first(DEFAULT_PAGE_SIZE);
    }

    /** Page size, 1 to {@value #MAX_PAGE_SIZE}. */
    public static ListOptions first(int first) {
        if (first < 1 || first > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("first must be between 1 and " + MAX_PAGE_SIZE + ", got " + first);
        }
        return new ListOptions(first, null, null, null, Collections.<String, VariableValue>emptyMap());
    }

    /** Start after this cursor (a previous page's {@link PageInfo#getEndCursor()}). */
    public ListOptions after(String cursor) {
        return new ListOptions(first, blankToNull(cursor), where, order, variables);
    }

    /** A GraphQL filter literal, e.g. {@code { disabled: { eq: false } }}. Replaces the operation's default filter. */
    public ListOptions where(String whereLiteral) {
        return new ListOptions(first, after, blankToNull(whereLiteral), order, variables);
    }

    /** A GraphQL sort literal, e.g. {@code [{ documentDate: DESC }]}. Replaces the operation's default order. */
    public ListOptions order(String orderLiteral) {
        return new ListOptions(first, after, where, blankToNull(orderLiteral), variables);
    }

    /**
     * Declares a variable referenced from {@code where}/{@code order}.
     *
     * @param name  without the {@code $}
     * @param type  its GraphQL type, e.g. {@code UUID!}, {@code String}, {@code DateTime}
     * @param value its value (anything the SDK can serialize)
     */
    public ListOptions variable(String name, String type, Object value) {
        if (name == null || !VARIABLE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid GraphQL variable name: " + name);
        }
        if ("first".equals(name) || "after".equals(name)) {
            throw new IllegalArgumentException("'" + name + "' is reserved for paging");
        }
        if (type == null || type.trim().isEmpty() || !type.matches("[\\[\\]!_0-9A-Za-z ]+")) {
            throw new IllegalArgumentException("Invalid GraphQL type for $" + name + ": " + type);
        }
        Map<String, VariableValue> copy = new LinkedHashMap<String, VariableValue>(variables);
        copy.put(name, new VariableValue(type.trim(), value));
        return new ListOptions(first, after, where, order, Collections.unmodifiableMap(copy));
    }

    public int getFirst() {
        return first;
    }

    public String getAfter() {
        return after;
    }

    public String getWhere() {
        return where;
    }

    public String getOrder() {
        return order;
    }

    /** Declared extra variables by name. */
    public Map<String, VariableValue> getVariables() {
        return variables;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /** A declared variable's GraphQL type and value. */
    public static final class VariableValue {
        private final String type;
        private final Object value;

        VariableValue(String type, Object value) {
            this.type = type;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }
    }

    @Override
    public String toString() {
        return "ListOptions{first=" + first + ", after=" + after + ", where=" + where + ", order=" + order
                + ", variables=" + variables.keySet() + '}';
    }
}
