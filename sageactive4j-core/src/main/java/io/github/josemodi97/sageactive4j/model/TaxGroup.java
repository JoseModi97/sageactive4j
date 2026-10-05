package io.github.josemodi97.sageactive4j.model;

import java.util.Map;

/** A tax group definition. */
public final class TaxGroup extends SageObject {

    public TaxGroup(Map<String, Object> json) {
        super(json);
    }

    public String getName() { return string("name"); }
    public String getTaxGroupCode() { return string("taxGroupCode"); }
    public String getTaxType() { return string("taxType"); }
    public String getVatTaxation() { return string("vatTaxation"); }
}
