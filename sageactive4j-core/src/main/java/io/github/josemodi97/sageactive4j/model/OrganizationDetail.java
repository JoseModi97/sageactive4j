package io.github.josemodi97.sageactive4j.model;

import java.util.List;
import java.util.Map;

/** The full configuration of the organization selected by {@code X-OrganizationId}. */
public final class OrganizationDetail extends SageObject {

    public OrganizationDetail(Map<String, Object> json) {
        super(json);
    }

    public String getSocialName() { return string("socialName"); }
    /** SIREN/SIRET (FR), Steuer-IdNr (DE), NIF (ES). */
    public String getDocumentId() { return string("documentId"); }
    public String getVatNumber() { return string("vatNumber"); }
    /** Cash VAT enabled (FR/DE). */
    public Boolean getVatCriterion() { return bool("vatCriterion"); }
    public String getNafApeCode() { return string("nafApeCode"); }
    public String getCurrencyId() { return string("currencyId"); }
    public Currency getCurrency() { return object(Currency::new, "currency"); }
    public List<Address> getAddresses() { return list(Address::new, "addresses"); }
}
