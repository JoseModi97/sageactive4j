package io.github.josemodi97.sageactive4j.model;

import java.util.List;
import java.util.Map;

/**
 * One aggregation the catalog offers. Every value used in an
 * {@code AggregationRequest} must come from here, verbatim.
 */
public final class AggregationDefinition extends SageObject {

    public AggregationDefinition(Map<String, Object> json) {
        super(json);
    }

    /** e.g. {@code queryAggregateSalesInvoices}. */
    public String getEntityKey() { return string("entityKey"); }
    /** e.g. {@code Sales Invoices by Customer}. */
    public String getContext() { return string("context"); }
    public List<String> getAggregationTypes() { return stringList("aggregationTypes"); }
    public List<String> getPeriodTypes() { return stringList("periodTypes"); }
    public List<String> getFilters() { return stringList("filters"); }
    public List<GroupBy> getGroupBy() { return list(GroupBy::new, "groupBy"); }
    public List<String> getValueColumns() { return stringList("valueColumns"); }
    public List<String> getDistinctCounts() { return stringList("distinctCounts"); }
    public List<String> getSearchScopes() { return stringList("searchScopes"); }

    /** A grouping option. */
    public static final class GroupBy extends SageObject {
        public GroupBy(Map<String, Object> json) {
            super(json);
        }

        public String getName() { return string("name"); }
        public String getTitle1() { return string("title1"); }
        public String getTitle2() { return string("title2"); }
    }
}
