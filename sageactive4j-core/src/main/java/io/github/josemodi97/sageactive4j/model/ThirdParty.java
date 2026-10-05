package io.github.josemodi97.sageactive4j.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Fields shared by customers and suppliers. */
public abstract class ThirdParty extends SageObject {

    protected ThirdParty(Map<String, Object> json) {
        super(json);
    }

    public String getCode() { return string("code"); }
    public String getSocialName() { return string("socialName"); }
    public String getTradeName() { return string("tradeName"); }
    /** ISO2 country code. */
    public String getCountryAcronym() { return string("countryAcronym"); }
    public String getDocumentId() { return string("documentId"); }
    public String getVatNumber() { return string("vatNumber"); }
    /** {@code ENABLED}, {@code DISABLED}, {@code GDPR_APPLIED}, {@code NONE}. */
    public String getStatus() { return string("status"); }
    public Boolean getDisabled() { return bool("disabled"); }
    public OffsetDateTime getCreationDate() { return dateTime("creationDate"); }
    public OffsetDateTime getModificationDate() { return dateTime("modificationDate"); }
    public List<Address> getAddresses() { return list(Address::new, "addresses"); }

    /** The address flagged {@code isMainAddress}, else the first one, else {@code null}. */
    public Address getMainAddress() {
        List<Address> addresses = getAddresses();
        for (Address address : addresses) {
            if (Boolean.TRUE.equals(address.getIsMainAddress())) {
                return address;
            }
        }
        return addresses.isEmpty() ? null : addresses.get(0);
    }
}
