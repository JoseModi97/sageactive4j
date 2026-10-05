package io.github.josemodi97.sageactive4j.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** A sales tariff definition. */
public final class SalesTariff extends SageObject {

    public SalesTariff(Map<String, Object> json) {
        super(json);
    }

    public String getCode() { return string("code"); }
    public String getName() { return string("name"); }
    public String getType() { return string("type"); }
    public Boolean isEnabled() { return bool("enabled"); }
    public LocalDate getStartDate() { return date("startDate"); }
    public LocalDate getEndDate() { return date("endDate"); }
    public OffsetDateTime getCreationDate() { return dateTime("creationDate"); }
    public OffsetDateTime getModificationDate() { return dateTime("modificationDate"); }
    public List<Line> getLines() { return list(Line::new, "lines"); }

    /** One line of a {@link SalesTariff}. */
    public static final class Line extends SageObject {
        public Line(Map<String, Object> json) {
            super(json);
        }

        public String getProductId() { return string("productId"); }
        public Boolean isEnabled() { return bool("enabled"); }
        public BigDecimal getIndicatorValue() { return decimal("indicatorValue"); }
        public LocalDate getStartDate() { return date("startDate"); }
        public LocalDate getEndDate() { return date("endDate"); }
    }
}
