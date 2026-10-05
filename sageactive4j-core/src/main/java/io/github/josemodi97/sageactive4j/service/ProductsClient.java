package io.github.josemodi97.sageactive4j.service;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.graphql.Pages;
import io.github.josemodi97.sageactive4j.input.ProductPriceRequest;
import io.github.josemodi97.sageactive4j.internal.GraphQLDocuments;
import io.github.josemodi97.sageactive4j.model.Product;
import io.github.josemodi97.sageactive4j.model.ProductPrice;
import io.github.josemodi97.sageactive4j.model.SalesTariff;
import java.util.LinkedHashMap;
import java.util.Map;

/** Products and computed prices. */
public final class ProductsClient extends DomainClient {

    public ProductsClient(SageActive4jClient client) {
        super(client);
    }

    /** Products by code. Filter e.g. {@code where("{ obsolete: { eq: false } }")}. */
    public Connection<Product> list(ListOptions options) {
        return list(organization("products"), "products", options, null, "[{ code: ASC }]", null, Product::new);
    }

    public Iterable<Product> all(ListOptions options) {
        return Pages.iterate(options, this::list);
    }

    /**
     * The price and discount Sage Active would apply for this customer,
     * date and quantity, after tariff and discount rules
     * ({@code productPriceById}).
     */
    public ProductPrice price(String productId, ProductPriceRequest request) {
        ProductPriceRequest context = request == null ? new ProductPriceRequest() : validated(request, "request");
        Map<String, Object> variables = new LinkedHashMap<String, Object>();
        variables.put("id", requireId(productId, "productId"));
        variables.put("productPrice", context);
        Map<String, Object> data = organization("productPriceById")
                .query(GraphQLDocuments.operation("productPriceById", variables));
        return new ProductPrice(require(data, "productPriceById"));
    }

    /** Sales tariffs by code. */
    public Connection<SalesTariff> tariffs(ListOptions options) {
        return list(organization("salesTariffs"), "salesTariffs", options, null, "[{ code: ASC }]", null,
                SalesTariff::new);
    }

    public Iterable<SalesTariff> allTariffs(ListOptions options) {
        return Pages.iterate(options, this::tariffs);
    }
}
