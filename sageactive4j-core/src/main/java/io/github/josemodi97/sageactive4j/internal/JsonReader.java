package io.github.josemodi97.sageactive4j.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A complete, dependency-free RFC 8259 JSON parser. GraphQL responses nest
 * arbitrarily deep (connections, edges, nodes, nested entities), so this is
 * a real recursive-descent parser rather than a flat-object scanner.
 *
 * <p>Mapping: object → {@link LinkedHashMap} (key order kept, duplicate keys:
 * last wins), array → {@link ArrayList}, string → {@link String}, integer
 * literal → {@link Long} (or {@link BigInteger} if it overflows), any number
 * with a fraction or exponent → {@link BigDecimal} (never {@code double},
 * so money keeps its exact value), {@code true}/{@code false} →
 * {@link Boolean}, {@code null} → {@code null}.
 *
 * <p>Not part of the public API.
 */
public final class JsonReader {

    /** Guards against stack exhaustion on hostile or corrupt input. */
    static final int MAX_DEPTH = 512;

    private final String in;
    private int pos;
    private int depth;

    private JsonReader(String in) {
        this.in = in;
    }

    /** Parses any JSON value. @throws JsonParseException on malformed input */
    public static Object parse(String json) {
        if (json == null) {
            throw new JsonParseException("Cannot parse null as JSON", 0);
        }
        JsonReader reader = new JsonReader(json);
        reader.skipWhitespace();
        Object value = reader.readValue();
        reader.skipWhitespace();
        if (reader.pos != json.length()) {
            throw reader.error("Unexpected trailing content");
        }
        return value;
    }

    /** Parses a JSON document whose top level must be an object. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object value = parse(json);
        if (!(value instanceof Map)) {
            throw new JsonParseException("Expected a JSON object at the top level, got "
                    + (value == null ? "null" : value.getClass().getSimpleName()), 0);
        }
        return (Map<String, Object>) value;
    }

    // ---------------------------------------------------------------- path helpers

    /**
     * Walks nested objects: {@code get(map, "customer", "code")}. Returns
     * {@code null} if any step is missing or not an object.
     */
    public static Object get(Map<String, Object> map, String... path) {
        Object current = map;
        for (String key : path) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(key);
        }
        return current;
    }

    public static String getString(Map<String, Object> map, String... path) {
        Object value = get(map, path);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        return value instanceof Map || value instanceof List ? null : value.toString();
    }

    public static BigDecimal getBigDecimal(Map<String, Object> map, String... path) {
        return toBigDecimal(get(map, path));
    }

    public static Long getLong(Map<String, Object> map, String... path) {
        Object value = get(map, path);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static Integer getInt(Map<String, Object> map, String... path) {
        Long value = getLong(map, path);
        return value == null ? null : value.intValue();
    }

    public static Boolean getBoolean(Map<String, Object> map, String... path) {
        Object value = get(map, path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String s = ((String) value).trim();
            if ("true".equalsIgnoreCase(s)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(s)) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> map, String... path) {
        Object value = get(map, path);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> map, String... path) {
        Object value = get(map, path);
        return value instanceof List ? (List<Object>) value : Collections.emptyList();
    }

    /** Converts any JSON number (or numeric string) to {@link BigDecimal}; {@code null} otherwise. */
    public static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof BigInteger) {
            return new BigDecimal((BigInteger) value);
        }
        if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return BigDecimal.valueOf(((Number) value).longValue());
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (value instanceof String) {
            try {
                return new BigDecimal(((String) value).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- parser

    private Object readValue() {
        if (pos >= in.length()) {
            throw error("Unexpected end of input");
        }
        char c = in.charAt(pos);
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
                return readString();
            case 't':
                expectLiteral("true");
                return Boolean.TRUE;
            case 'f':
                expectLiteral("false");
                return Boolean.FALSE;
            case 'n':
                expectLiteral("null");
                return null;
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return readNumber();
                }
                throw error("Unexpected character '" + c + "'");
        }
    }

    private Map<String, Object> readObject() {
        enter();
        pos++; // {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            depth--;
            return map;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("Expected a string key");
            }
            String key = readString();
            skipWhitespace();
            if (peek() != ':') {
                throw error("Expected ':' after object key");
            }
            pos++;
            skipWhitespace();
            map.put(key, readValue());
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
            } else if (c == '}') {
                pos++;
                depth--;
                return map;
            } else {
                throw error("Expected ',' or '}' in object");
            }
        }
    }

    private List<Object> readArray() {
        enter();
        pos++; // [
        List<Object> list = new ArrayList<Object>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            depth--;
            return list;
        }
        while (true) {
            skipWhitespace();
            list.add(readValue());
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
            } else if (c == ']') {
                pos++;
                depth--;
                return list;
            } else {
                throw error("Expected ',' or ']' in array");
            }
        }
    }

    private String readString() {
        pos++; // opening quote
        StringBuilder sb = null;
        int runStart = pos;
        while (true) {
            if (pos >= in.length()) {
                throw error("Unterminated string");
            }
            char c = in.charAt(pos);
            if (c == '"') {
                String result = sb == null
                        ? in.substring(runStart, pos)
                        : sb.append(in, runStart, pos).toString();
                pos++;
                return result;
            }
            if (c < 0x20) {
                throw error("Unescaped control character in string");
            }
            if (c == '\\') {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(in, runStart, pos);
                pos++;
                if (pos >= in.length()) {
                    throw error("Unterminated escape sequence");
                }
                char e = in.charAt(pos);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 >= in.length()) {
                            throw error("Truncated \\u escape");
                        }
                        try {
                            sb.append((char) Integer.parseInt(in.substring(pos + 1, pos + 5), 16));
                        } catch (NumberFormatException ex) {
                            throw error("Invalid \\u escape");
                        }
                        pos += 4;
                        break;
                    default:
                        throw error("Invalid escape '\\" + e + "'");
                }
                pos++;
                runStart = pos;
                continue;
            }
            pos++;
        }
    }

    private Object readNumber() {
        int start = pos;
        boolean decimal = false;
        if (peek() == '-') {
            pos++;
        }
        if (peek() == '0') {
            pos++;
            if (isDigit(peek())) {
                throw error("Leading zeros are not allowed");
            }
        } else if (isDigit(peek())) {
            while (isDigit(peek())) {
                pos++;
            }
        } else {
            throw error("Expected a digit");
        }
        if (peek() == '.') {
            decimal = true;
            pos++;
            if (!isDigit(peek())) {
                throw error("Expected a digit after the decimal point");
            }
            while (isDigit(peek())) {
                pos++;
            }
        }
        if (peek() == 'e' || peek() == 'E') {
            decimal = true;
            pos++;
            if (peek() == '+' || peek() == '-') {
                pos++;
            }
            if (!isDigit(peek())) {
                throw error("Expected a digit in the exponent");
            }
            while (isDigit(peek())) {
                pos++;
            }
        }
        String literal = in.substring(start, pos);
        if (decimal) {
            return new BigDecimal(literal);
        }
        if (literal.length() < 19) {
            return Long.parseLong(literal);
        }
        BigInteger big = new BigInteger(literal);
        return big.bitLength() < 64 ? (Object) big.longValue() : big;
    }

    private void expectLiteral(String literal) {
        if (!in.startsWith(literal, pos)) {
            throw error("Invalid literal");
        }
        pos += literal.length();
    }

    private void enter() {
        if (++depth > MAX_DEPTH) {
            throw error("Nesting deeper than " + MAX_DEPTH + " levels");
        }
    }

    private void skipWhitespace() {
        while (pos < in.length()) {
            char c = in.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                return;
            }
        }
    }

    private char peek() {
        return pos < in.length() ? in.charAt(pos) : '\0';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private JsonParseException error(String message) {
        return new JsonParseException(message + " at position " + pos, pos);
    }

    /** Malformed JSON. Not part of the public API. */
    public static final class JsonParseException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final int position;

        JsonParseException(String message, int position) {
            super(message);
            this.position = position;
        }

        public int getPosition() {
            return position;
        }
    }
}
