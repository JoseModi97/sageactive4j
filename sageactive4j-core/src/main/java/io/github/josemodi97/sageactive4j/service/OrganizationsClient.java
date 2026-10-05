package io.github.josemodi97.sageactive4j.service;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.exception.SageActive4jApiException;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.internal.GraphQLDocuments;
import io.github.josemodi97.sageactive4j.model.Country;
import io.github.josemodi97.sageactive4j.model.Currency;
import io.github.josemodi97.sageactive4j.model.Organization;
import io.github.josemodi97.sageactive4j.model.OrganizationDetail;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Organizations (businesses) and shared reference data: countries, currencies. */
public final class OrganizationsClient extends DomainClient {

    public OrganizationsClient(SageActive4jClient client) {
        super(client);
    }

    /** Organizations the signed-in user can access, newest first. Needs no organization. */
    public Connection<Organization> list(ListOptions options) {
        return list(global(), "organizations", options, null, "[{ creationDate: DESC }]", null, Organization::new);
    }

    /**
     * Only the organizations the API may be used against ({@code READY} and
     * onboarded) - the ids to pass to {@code withOrganization(...)}.
     */
    public List<Organization> listUsable() {
        List<Organization> usable = new ArrayList<Organization>();
        for (Organization organization : listAll(global(), "organizations", null, null, Organization::new)) {
            if (organization.isUsable()) {
                usable.add(organization);
            }
        }
        return Collections.unmodifiableList(usable);
    }

    /** The full configuration of the current organization ({@code organizationDetail}). */
    public OrganizationDetail getDetail() {
        Map<String, Object> data = organization("organizationDetail")
                .query(GraphQLDocuments.operation("organizationDetail", null));
        Connection<OrganizationDetail> details = Connection.fromJson(require(data, "organizationDetail"), OrganizationDetail::new);
        if (details.isEmpty()) {
            throw new SageActive4jApiException("Sage Active returned no organizationDetail for organization "
                    + client.getOrganizationId(), SageActive4jApiException.NO_HTTP_STATUS, null);
        }
        return details.getNodes().get(0);
    }

    /** Countries, by name. Sage Active resolves the legislation from the organization. */
    public Connection<Country> countries(ListOptions options) {
        return list(organization("countries"), "countries", options, null, "[{ name: ASC }]", null, Country::new);
    }

    public Connection<Currency> currencies(ListOptions options) {
        return list(organization("currencies"), "currencies", options, null, null, null, Currency::new);
    }
}
