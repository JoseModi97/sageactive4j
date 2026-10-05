package io.github.josemodi97.sageactive4j.input;

/** An address for a new customer or supplier. */
public final class AddressInput extends SageInput<AddressInput> {

    public AddressInput name(String name) { return set("name", name); }
    public AddressInput firstLine(String firstLine) { return set("firstLine", firstLine); }
    public AddressInput secondLine(String secondLine) { return set("secondLine", secondLine); }
    public AddressInput city(String city) { return set("city", city); }
    public AddressInput zipCode(String zipCode) { return set("zipCode", zipCode); }
    /** Required by Spanish legislation when closing invoices. */
    public AddressInput province(String province) { return set("province", province); }
    /** ISO2 code, e.g. {@code FR}; use {@code XI} for Northern Ireland. */
    public AddressInput countryIsoCodeAlpha2(String countryIsoCodeAlpha2) { return set("countryIsoCodeAlpha2", countryIsoCodeAlpha2); }
}
