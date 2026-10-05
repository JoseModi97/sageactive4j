package io.github.josemodi97.sageactive4j.model;

import java.util.Map;

/**
 * What a create mutation returned: always the {@code id}, plus the
 * assigned {@code code} (customers) or {@code number} (accounting entries).
 */
public final class CreatedRecord extends SageObject {

    public CreatedRecord(Map<String, Object> json) {
        super(json);
    }

    /** The code Sage Active assigned (e.g. an automatic customer code), or {@code null}. */
    public String getCode() { return string("code"); }

    /** The sequential number Sage Active assigned (e.g. an accounting entry number), or {@code null}. */
    public Long getNumber() { return longValue("number"); }
}
