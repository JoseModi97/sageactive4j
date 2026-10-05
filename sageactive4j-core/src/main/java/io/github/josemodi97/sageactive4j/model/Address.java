package io.github.josemodi97.sageactive4j.model;

import java.util.Map;

/** A postal address of a customer, supplier or organization. */
public final class Address extends SageObject {

    public Address(Map<String, Object> json) {
        super(json);
    }

    public String getName() { return string("name"); }
    public String getFirstLine() { return string("firstLine"); }
    public String getSecondLine() { return string("secondLine"); }
    public String getCity() { return string("city"); }
    public String getZipCode() { return string("zipCode"); }
    /** Province / region / federal state. */
    public String getProvince() { return string("province"); }
    public String getCountryIsoCodeAlpha2() { return string("countryIsoCodeAlpha2"); }
    public String getCountryName() { return string("countryName"); }
    public Boolean getIsMainAddress() { return bool("isMainAddress"); }
}
