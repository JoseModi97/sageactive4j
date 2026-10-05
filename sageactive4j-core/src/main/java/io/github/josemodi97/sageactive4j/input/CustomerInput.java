package io.github.josemodi97.sageactive4j.input;

/**
 * A new customer ({@code CustomerCreateGLDtoInput}). Requires a social name
 * and an address.
 *
 * <p>Leave {@link #code(String)} unset when the organization numbers
 * customers automatically (always the case in DE); a forced code must be
 * numeric, 10000-69999.
 */
public final class CustomerInput extends SageInput<CustomerInput> {

    public CustomerInput code(String code) { return set("code", code); }
    public CustomerInput socialName(String socialName) { return set("socialName", socialName); }
    public CustomerInput tradeName(String tradeName) { return set("tradeName", tradeName); }
    /** SIREN/SIRET (FR), NIF/DNI (ES), Steuer-IdNr (DE). Required in practice for ES invoicing. */
    public CustomerInput documentId(String documentId) { return set("documentId", documentId); }
    /** EU VAT number; not used for customers outside the VIES area. */
    public CustomerInput vatNumber(String vatNumber) { return set("vatNumber", vatNumber); }
    /** {@code BUSINESS} (default) or {@code INDIVIDUAL}. */
    public CustomerInput customerType(String customerType) { return set("customerType", customerType); }
    /** ISO2; derived from the address when unset. */
    public CustomerInput countryAcronym(String countryAcronym) { return set("countryAcronym", countryAcronym); }

    public CustomerInput addAddress(AddressInput address) {
        return add("addresses", address);
    }

    @Override
    public void validate() {
        requireFields("socialName", "addresses");
    }
}
