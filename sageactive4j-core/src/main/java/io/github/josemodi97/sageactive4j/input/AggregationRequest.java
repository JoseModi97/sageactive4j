package io.github.josemodi97.sageactive4j.input;

import java.time.LocalDate;

/**
 * An analytics aggregation ({@code AggregationExecuteDtoInput}). Take every
 * name ({@code entityKey}, {@code groupByName}, value columns, ...) verbatim
 * from {@code catalog().aggregationCatalog(...)}; anything else is rejected.
 */
public final class AggregationRequest extends SageInput<AggregationRequest> {

    /** e.g. {@code queryAggregateSalesInvoices}. */
    public AggregationRequest entityKey(String entityKey) { return set("entityKey", entityKey); }
    /** e.g. {@code SUM}, {@code AVG}, {@code COUNT}. */
    public AggregationRequest aggregationType(String aggregationType) { return set("aggregationType", aggregationType); }
    /** e.g. {@code Month}, {@code Year}. */
    public AggregationRequest periodType(String periodType) { return set("periodType", periodType); }
    public AggregationRequest dateMin(LocalDate dateMin) { return set("dateMin", dateMin); }
    public AggregationRequest dateMax(LocalDate dateMax) { return set("dateMax", dateMax); }
    /** Compare with the previous period (N / N-1). */
    public AggregationRequest compare(boolean compare) { return set("compare", compare); }
    /** Keep only the top N groups. */
    public AggregationRequest top(int top) { return set("top", top); }
    /** Add a row summing everything outside the top N. */
    public AggregationRequest includeOthers(boolean includeOthers) { return set("includeOthers", includeOthers); }
    public AggregationRequest othersLabel(String othersLabel) { return set("othersLabel", othersLabel); }
    public AggregationRequest filterName(String filterName) { return set("filterName", filterName); }
    public AggregationRequest groupByName(String groupByName) { return set("groupByName", groupByName); }
    public AggregationRequest valueColumn1(String valueColumn1) { return set("valueColumn1", valueColumn1); }
    public AggregationRequest valueColumn2(String valueColumn2) { return set("valueColumn2", valueColumn2); }
    public AggregationRequest distinctCountName(String distinctCountName) { return set("distinctCountName", distinctCountName); }
    public AggregationRequest search(String search) { return set("search", search); }
    public AggregationRequest searchScope(String searchScope) { return set("searchScope", searchScope); }

    @Override
    public void validate() {
        requireFields("entityKey", "aggregationType");
    }
}
