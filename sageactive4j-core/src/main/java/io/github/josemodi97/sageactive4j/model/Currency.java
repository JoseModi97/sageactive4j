package io.github.josemodi97.sageactive4j.model;

import java.util.Map;

/** A currency definition. */
public final class Currency extends SageObject {

    public Currency(Map<String, Object> json) {
        super(json);
    }

    /** ISO 4217 code, e.g. {@code EUR}. */
    public String getCode() { return string("code"); }
    public String getDescription() { return string("description"); }
    /** Decimal places used for amounts. */
    public Integer getPrecision() { return integer("precision"); }
}
