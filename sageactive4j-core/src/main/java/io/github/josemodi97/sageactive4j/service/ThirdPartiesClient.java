package io.github.josemodi97.sageactive4j.service;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.graphql.Pages;
import io.github.josemodi97.sageactive4j.input.CustomerInput;
import io.github.josemodi97.sageactive4j.internal.GraphQLDocuments;
import io.github.josemodi97.sageactive4j.model.CreatedRecord;
import io.github.josemodi97.sageactive4j.model.Customer;
import io.github.josemodi97.sageactive4j.model.Supplier;
import java.util.Collections;
import java.util.Map;

/** Customers and suppliers. */
public final class ThirdPartiesClient extends DomainClient {

    public ThirdPartiesClient(SageActive4jClient client) {
        super(client);
    }

    /** Customers with addresses and contacts, by code. Filter e.g. {@code where("{ disabled: { eq: false } }")}. */
    public Connection<Customer> customers(ListOptions options) {
        return list(organization("customers"), "customers", options, null, "[{ code: ASC }]", null, Customer::new);
    }

    public Iterable<Customer> allCustomers(ListOptions options) {
        return Pages.iterate(options, this::customers);
    }

    /** Creates a customer; returns its id and (possibly automatic) code. */
    public CreatedRecord createCustomer(CustomerInput customer) {
        Map<String, Object> data = organization("createCustomer").query(GraphQLDocuments.operation("createCustomer",
                Collections.singletonMap("values", validated(customer, "customer"))));
        return new CreatedRecord(require(data, "createCustomer"));
    }

    public Connection<Supplier> suppliers(ListOptions options) {
        return list(organization("suppliers"), "suppliers", options, null, "[{ code: ASC }]", null, Supplier::new);
    }

    public Iterable<Supplier> allSuppliers(ListOptions options) {
        return Pages.iterate(options, this::suppliers);
    }
}
