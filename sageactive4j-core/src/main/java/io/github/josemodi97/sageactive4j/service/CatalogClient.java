package io.github.josemodi97.sageactive4j.service;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.input.AggregationRequest;
import io.github.josemodi97.sageactive4j.internal.GraphQLDocuments;
import io.github.josemodi97.sageactive4j.model.AggregationDefinition;
import io.github.josemodi97.sageactive4j.model.AggregationResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Analytics: discover aggregations ("Sales Invoices by Customer", "Accounting
 * Entry Lines by Account", ...) and run them, with period comparison and top N.
 */
public final class CatalogClient extends DomainClient {

    public CatalogClient(SageActive4jClient client) {
        super(client);
    }

    /**
     * Available aggregations and their allowed parameters.
     *
     * @param contextContains e.g. {@code "Sales"}; {@code null} = all
     */
    public List<AggregationDefinition> aggregationCatalog(String contextContains) {
        Map<String, Object> data = global().query(GraphQLDocuments.operation("aggregationCatalog",
                Collections.singletonMap("contextContains", contextContains)));
        return new Holder(require(data, "aggregationCatalog")).definitions();
    }

    /** Runs an aggregation (at most 500 rows; no paging). */
    public AggregationResult aggregate(AggregationRequest request) {
        Map<String, Object> data = organization("aggregationExecute").query(GraphQLDocuments.operation(
                "aggregationExecute", Collections.singletonMap("input", validated(request, "request"))));
        return new AggregationResult(require(data, "aggregationExecute"));
    }

    private static final class Holder extends io.github.josemodi97.sageactive4j.model.SageObject {
        Holder(Map<String, Object> json) {
            super(json);
        }

        List<AggregationDefinition> definitions() {
            return list(AggregationDefinition::new, "definitions");
        }
    }
}
