package io.github.josemodi97.sageactive4j.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** A tax definition. */
public final class Tax extends SageObject {

    public Tax(Map<String, Object> json) {
        super(json);
    }

    public String getName() { return string("name"); }
    public String getGroupName() { return string("groupName"); }
    public String getGroupId() { return string("groupId"); }
    public BigDecimal getPercentage() { return decimal("percentage"); }
    public BigDecimal getEquivalenceSurchargePercentage() { return decimal("equivalenceSurchargePercentage"); }
    public Boolean getHasEquivalenceSurcharge() { return bool("hasEquivalenceSurcharge"); }
    public String getTaxType() { return string("taxType"); }
    public Boolean isInactive() { return bool("inactive"); }
    public LocalDate getEffectiveDate() { return date("effectiveDate"); }
    public LocalDate getInactivationDate() { return date("inactivationDate"); }
}
