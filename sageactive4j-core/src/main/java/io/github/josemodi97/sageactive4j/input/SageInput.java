package io.github.josemodi97.sageactive4j.input;

import io.github.josemodi97.sageactive4j.internal.JsonSerializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base of the SDK's mutation and query inputs: a fluent builder over the
 * JSON object sent to Sage Active. Typed setters cover documented fields;
 * {@link #set(String, Object)} sends anything else (a field added in a newer
 * Sage release, a legislation-specific one) without waiting for an SDK update.
 *
 * <p>Setting a value to {@code null} removes the field. Inputs are mutable
 * and not thread-safe: build one per request.
 */
public abstract class SageInput<SELF extends SageInput<SELF>> implements JsonSerializable {

    private final Map<String, Object> fields = new LinkedHashMap<String, Object>();

    /** Sets any field by its Sage Active name. */
    public SELF set(String field, Object value) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("field name must not be blank");
        }
        if (value == null) {
            fields.remove(field);
        } else {
            fields.put(field, value);
        }
        return self();
    }

    /** The current value of a field, or {@code null}. */
    public Object get(String field) {
        return fields.get(field);
    }

    /** A snapshot of the fields as they will be sent. */
    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(fields));
    }

    /**
     * Checks the fields Sage Active requires, before any request is sent.
     *
     * @throws IllegalArgumentException naming every missing field
     */
    public void validate() {
        // Subclasses list their required fields.
    }

    protected void requireFields(String... names) {
        List<String> missing = new ArrayList<String>();
        for (String name : names) {
            Object value = fields.get(name);
            if (value == null || (value instanceof CharSequence && value.toString().trim().isEmpty())
                    || (value instanceof List && ((List<?>) value).isEmpty())) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " is missing required field(s): " + missing);
        }
    }

    /** Appends to a list-valued field. */
    @SuppressWarnings("unchecked")
    protected SELF add(String listField, Object item) {
        if (item == null) {
            throw new IllegalArgumentException(listField + " item must not be null");
        }
        Object existing = fields.get(listField);
        List<Object> list;
        if (existing instanceof List) {
            list = (List<Object>) existing;
        } else {
            list = new ArrayList<Object>();
            fields.put(listField, list);
        }
        list.add(item);
        return self();
    }

    @Override
    public Object toJsonValue() {
        return toMap();
    }

    @SuppressWarnings("unchecked")
    private SELF self() {
        return (SELF) this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + fields;
    }
}
