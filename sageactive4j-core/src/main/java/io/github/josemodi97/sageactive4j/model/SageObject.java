package io.github.josemodi97.sageactive4j.model;

import io.github.josemodi97.sageactive4j.internal.JsonReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Base of every Sage Active entity returned by the SDK: an immutable view
 * over the JSON object Sage Active sent, with typed getters for documented
 * fields.
 *
 * <p>Getters return {@code null} for fields the query didn't select or Sage
 * Active didn't return, rather than failing - a schema addition or a custom
 * selection never breaks parsing. Anything without a typed getter is
 * available from {@link #getRaw()} / {@link #get(String...)}.
 */
public abstract class SageObject {

    private final Map<String, Object> json;

    protected SageObject(Map<String, Object> json) {
        this.json = json == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(json));
    }

    /** The JSON object exactly as received (nested maps and lists). */
    public Map<String, Object> getRaw() {
        return json;
    }

    /** Any field by path, e.g. {@code get("customer", "code")}; {@code null} if absent. */
    public Object get(String... path) {
        return JsonReader.get(json, path);
    }

    /** {@code id} - present on almost every Sage Active entity. */
    public String getId() {
        return string("id");
    }

    protected String string(String... path) {
        return JsonReader.getString(json, path);
    }

    protected BigDecimal decimal(String... path) {
        return JsonReader.getBigDecimal(json, path);
    }

    protected Long longValue(String... path) {
        return JsonReader.getLong(json, path);
    }

    protected Integer integer(String... path) {
        return JsonReader.getInt(json, path);
    }

    protected Boolean bool(String... path) {
        return JsonReader.getBoolean(json, path);
    }

    /** A date: accepts {@code 2026-10-05} and date-times like {@code 2026-10-05T00:00:00Z} (time dropped). */
    protected LocalDate date(String... path) {
        return parseDate(string(path));
    }

    /** A timestamp: accepts offset date-times, local date-times (taken as UTC) and plain dates (UTC midnight). */
    protected OffsetDateTime dateTime(String... path) {
        return parseDateTime(string(path));
    }

    protected <T> T object(Function<Map<String, Object>, T> factory, String... path) {
        Map<String, Object> value = JsonReader.getMap(json, path);
        return value == null ? null : factory.apply(value);
    }

    @SuppressWarnings("unchecked")
    protected <T> List<T> list(Function<Map<String, Object>, T> factory, String... path) {
        List<Object> values = JsonReader.getList(json, path);
        List<T> result = new ArrayList<T>(values.size());
        for (Object value : values) {
            if (value instanceof Map) {
                result.add(factory.apply((Map<String, Object>) value));
            }
        }
        return Collections.unmodifiableList(result);
    }

    protected List<String> stringList(String... path) {
        List<Object> values = JsonReader.getList(json, path);
        List<String> result = new ArrayList<String>(values.size());
        for (Object value : values) {
            if (value != null) {
                result.add(value.toString());
            }
        }
        return Collections.unmodifiableList(result);
    }

    static LocalDate parseDate(String value) {
        if (value == null || value.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static OffsetDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException notOffset) {
            try {
                return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException notLocal) {
                LocalDate date = parseDate(value);
                return date == null ? null : date.atStartOfDay().atOffset(ZoneOffset.UTC);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        return other != null && other.getClass() == getClass() && json.equals(((SageObject) other).json);
    }

    @Override
    public int hashCode() {
        return json.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + json;
    }
}
