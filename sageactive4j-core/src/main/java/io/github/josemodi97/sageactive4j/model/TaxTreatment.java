package io.github.josemodi97.sageactive4j.model;

import java.util.Map;

/** A tax treatment definition. */
public final class TaxTreatment extends SageObject {

    public TaxTreatment(Map<String, Object> json) {
        super(json);
    }

    public String getDescription() { return string("description"); }
    public String getTaxCode() { return string("taxCode"); }
    public Boolean isInactive() { return bool("inactive"); }
    public Boolean isIntracomunity() { return bool("isIntracomunity"); }
    public String getRegisterType() { return string("registerType"); }
    public String getTaxGroupId() { return string("taxGroupId"); }
    public String getTaxType() { return string("taxType"); }
}
