package io.github.josemodi97.sageactive4j.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Iterator;
import java.util.Map;

/**
 * Dependency-free JSON serializer for GraphQL request bodies and variables.
 *
 * <p>Supports {@code null}, {@link CharSequence}, {@link Boolean},
 * {@link Number} ({@link BigDecimal} via {@code toPlainString()}, so amounts
 * never turn into {@code 1.5E+2}), {@link Map} (keys via {@code toString()}),
 * {@link Iterable}, {@code Object[]}, enums (by {@code name()}), java.time
 * values (ISO-8601 via {@code toString()}), and {@link JsonSerializable}.
 * Anything else is rejected rather than silently stringified.
 *
 * <p>Not part of the public API.
 */
public final class JsonWriter {

    private JsonWriter() {
    }

    /** Serializes {@code value} compactly. */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, null, 0);
        return sb.toString();
    }

    /** Serializes {@code value} with two-space indentation, for human-facing output (e.g. the CLI). */
    public static String writePretty(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, "  ", 0);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value, String indent, int level) {
        if (level > JsonReader.MAX_DEPTH) {
            throw new IllegalArgumentException("Value nests deeper than " + JsonReader.MAX_DEPTH
                    + " levels (a cyclic structure?)");
        }
        if (value == null) {
            sb.append("null");
        } else if (value instanceof JsonSerializable) {
            writeValue(sb, ((JsonSerializable) value).toJsonValue(), indent, level + 1);
        } else if (value instanceof CharSequence || value instanceof Character) {
            writeString(sb, value.toString());
        } else if (value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof BigDecimal) {
            sb.append(((BigDecimal) value).toPlainString());
        } else if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("JSON cannot represent " + d);
            }
            sb.append(new BigDecimal(value.toString()).toPlainString());
        } else if (value instanceof Number) {
            sb.append(value.toString());
        } else if (value instanceof Enum) {
            writeString(sb, ((Enum<?>) value).name());
        } else if (value instanceof TemporalAccessor) {
            writeString(sb, formatTemporal((TemporalAccessor) value));
        } else if (value instanceof Map) {
            writeObject(sb, (Map<?, ?>) value, indent, level);
        } else if (value instanceof Iterable) {
            writeArray(sb, ((Iterable<?>) value).iterator(), indent, level);
        } else if (value instanceof Object[]) {
            writeArray(sb, java.util.Arrays.asList((Object[]) value).iterator(), indent, level);
        } else {
            throw new IllegalArgumentException("Cannot serialize " + value.getClass().getName()
                    + " to JSON; use a Map, a List, a primitive wrapper, or implement JsonSerializable");
        }
    }

    /**
     * ISO-8601 with seconds always present: {@code toString()} on the
     * java.time types drops {@code :00} seconds ({@code 08:30Z}), which strict
     * GraphQL {@code DateTime} scalars reject. Zoned values are sent as
     * offsets, since the GraphQL scalar has no notion of a region ID.
     */
    private static String formatTemporal(TemporalAccessor value) {
        if (value instanceof OffsetDateTime) {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value);
        }
        if (value instanceof ZonedDateTime) {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value);
        }
        if (value instanceof Instant) {
            return DateTimeFormatter.ISO_INSTANT.format(value);
        }
        if (value instanceof LocalDateTime) {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value);
        }
        if (value instanceof LocalTime) {
            return DateTimeFormatter.ISO_LOCAL_TIME.format(value);
        }
        return value.toString();
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> map, String indent, int level) {
        if (map.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            newline(sb, indent, level + 1);
            writeString(sb, String.valueOf(entry.getKey()));
            sb.append(indent == null ? ":" : ": ");
            writeValue(sb, entry.getValue(), indent, level + 1);
        }
        newline(sb, indent, level);
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, Iterator<?> it, String indent, int level) {
        if (!it.hasNext()) {
            sb.append("[]");
            return;
        }
        sb.append('[');
        boolean first = true;
        while (it.hasNext()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            newline(sb, indent, level + 1);
            writeValue(sb, it.next(), indent, level + 1);
        }
        newline(sb, indent, level);
        sb.append(']');
    }

    private static void newline(StringBuilder sb, String indent, int level) {
        if (indent == null) {
            return;
        }
        sb.append('\n');
        for (int i = 0; i < level; i++) {
            sb.append(indent);
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    // Control characters must be escaped; U+2028/U+2029 are
                    // escaped too so the output is also safe inside JavaScript.
                    if (c < 0x20 || c == ' ' || c == ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
