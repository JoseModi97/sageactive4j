package io.github.josemodi97.sageactive4j.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** The rows of an executed aggregation (at most 500), plus the parameters it ran with. */
public final class AggregationResult extends SageObject {

    public AggregationResult(Map<String, Object> json) {
        super(json);
    }

    /** The effective parameters, as returned by Sage Active ({@code entityKey}, {@code periodType}, ...). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMeta() {
        Object meta = get("meta");
        return meta instanceof Map ? (Map<String, Object>) meta : java.util.Collections.<String, Object>emptyMap();
    }

    public List<Row> getRows() { return list(Row::new, "rows"); }

    /** One aggregated row. With {@code compare = true}, the {@code ...N}/{@code ...N_1} fields hold both periods. */
    public static final class Row extends SageObject {
        public Row(Map<String, Object> json) {
            super(json);
        }

        public String getGroupValue() { return string("groupValue"); }
        public String getSecondValue() { return string("secondValue"); }
        public String getPeriod() { return string("period"); }
        public BigDecimal getValue1() { return decimal("value1"); }
        public BigDecimal getValue2() { return decimal("value2"); }
        public String getPeriodN() { return string("periodN"); }
        public String getPeriodN1() { return string("periodN_1"); }
        public BigDecimal getValueN() { return decimal("valueN"); }
        public BigDecimal getValueN1() { return decimal("valueN_1"); }
        /** Evolution between the two periods, in percent. */
        public BigDecimal getDeltaPercent() { return decimal("deltaPercent"); }
    }
}
