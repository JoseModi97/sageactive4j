package io.github.josemodi97.sageactive4j.model;

import java.util.Map;

/** A country, with its ISO codes and VIES VAT prefix. */
public final class Country extends SageObject {

    public Country(Map<String, Object> json) {
        super(json);
    }

    public String getName() { return string("name"); }
    public String getIsoCodeAlpha2() { return string("isoCodeAlpha2"); }
    public String getIsoCodeAlpha3() { return string("isoCodeAlpha3"); }
    public String getIsoNumber() { return string("isoNumber"); }
    /** EU VAT number prefix ({@code EL} for Greece, {@code XI} for Northern Ireland); {@code null} outside the EU. */
    public String getViesCode() { return string("viesCode"); }
}
