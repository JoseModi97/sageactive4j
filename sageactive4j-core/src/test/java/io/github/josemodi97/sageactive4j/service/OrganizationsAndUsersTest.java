package io.github.josemodi97.sageactive4j.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import io.github.josemodi97.sageactive4j.model.AccessCheck;
import io.github.josemodi97.sageactive4j.model.Country;
import io.github.josemodi97.sageactive4j.model.Organization;
import io.github.josemodi97.sageactive4j.model.OrganizationDetail;
import io.github.josemodi97.sageactive4j.model.UserProfile;
import io.github.josemodi97.sageactive4j.testsupport.DomainTestSupport;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrganizationsAndUsersTest extends DomainTestSupport {

    private static final String ORGS = "{\"organizations\":{\"nodes\":["
            + "{\"id\":\"a\",\"socialName\":\"Ready Co\",\"legislationCode\":\"FR\",\"status\":\"READY\",\"onboardingCompleted\":true},"
            + "{\"id\":\"00000000-0000-0000-0000-000000000000\",\"socialName\":\"Onboarding Co\",\"status\":\"READY\",\"onboardingCompleted\":false},"
            + "{\"id\":\"00000000-0000-0000-0000-000000000000\",\"socialName\":\"Expired Co\",\"status\":\"EXPIRED\",\"onboardingCompleted\":true}"
            + "],\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":3}}";

    @Test
    void organizationsAreListedWithoutAnOrganizationHeader() {
        respond(ORGS);
        List<Organization> orgs = sage.organizations().list(ListOptions.defaults()).getNodes();

        assertEquals(3, orgs.size());
        assertNull(lastRequest().header("X-OrganizationId"), "organizations is a cross-organization query");
        assertContains(query(lastRequest()), "order: [{ creationDate: DESC }]");
    }

    @Test
    void onlyReadyOnboardedOrganizationsAreUsable() {
        respond(ORGS);
        List<Organization> usable = sage.organizations().listUsable();
        assertEquals(1, usable.size());
        assertEquals("Ready Co", usable.get(0).getSocialName());
    }

    @Test
    void organizationDetailIsAConnectionResolvedFromTheHeader() {
        respond("{\"organizationDetail\":{\"nodes\":[{\"id\":\"org-1\",\"documentId\":\"323456789\",\"vatNumber\":\"FR33323456789\","
                + "\"currency\":{\"code\":\"EUR\",\"precision\":2},\"addresses\":[{\"city\":\"PARIS\",\"isMainAddress\":true}]}]}}");

        OrganizationDetail detail = sage.organizations().getDetail();

        assertEquals("FR33323456789", detail.getVatNumber());
        assertEquals("EUR", detail.getCurrency().getCode());
        assertEquals(Integer.valueOf(2), detail.getCurrency().getPrecision());
        assertEquals("PARIS", detail.getAddresses().get(0).getCity());
        assertEquals(ORG, lastRequest().header("X-OrganizationId"));
        assertContains(query(lastRequest()), "organizationDetail(first: 1) { nodes {");
    }

    @Test
    void organizationScopedCallsFailFastWithoutAnOrganization() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> sage.withOrganization(null).organizations().getDetail());
        assertTrue(e.getMessage().contains("withOrganization"), e.getMessage());
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void countries() {
        respond("{\"countries\":{\"nodes\":[{\"name\":\"Grèce\",\"isoCodeAlpha2\":\"GR\",\"viesCode\":\"EL\"}],"
                + "\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");
        Country greece = sage.organizations().countries(ListOptions.defaults()).getNodes().get(0);
        assertEquals("Grèce", greece.getName());
        assertEquals("EL", greece.getViesCode());
    }

    @Test
    void userProfileNeedsNoOrganization() {
        respond("{\"userProfile\":{\"id\":\"u1\",\"fullName\":\"Ada Lovelace\",\"authenticationEmail\":\"ada@example.com\","
                + "\"applicationLanguageCode\":\"fr-FR\"}}");

        UserProfile me = sage.users().getProfile();

        assertEquals("Ada Lovelace", me.getFullName());
        assertEquals("fr-FR", me.getApplicationLanguageCode());
        assertNull(lastRequest().header("X-OrganizationId"));
        assertFalse(query(lastRequest()).contains("userId"), "userProfile has no userId field (HTTP 400 if selected)");
    }

    @Test
    void accessPolicyCheckMatchesTheDocumentedExample() {
        respond("{\"userAccessPolicyCheck\":[{\"action\":\"createCustomer\",\"isAllowed\":true},"
                + "{\"action\":\"deleteSalesQuote\",\"isAllowed\":false},{\"action\":\"accountingAccounts\",\"isAllowed\":true}]}");

        List<AccessCheck> checks = sage.users().checkAccess("createCustomer", "deleteSalesQuote", "accountingAccounts");

        assertEquals(3, checks.size());
        assertTrue(checks.get(0).isAllowed());
        assertFalse(checks.get(1).isAllowed());
        assertEquals(Arrays.<Object>asList("createCustomer", "deleteSalesQuote", "accountingAccounts"),
                JsonReader.getList(variables(lastRequest()), "actions"));
    }

    @Test
    void isAllowedIsFalseUnlessExplicitlyAllowed() {
        respond("{\"userAccessPolicyCheck\":[]}");
        assertFalse(sage.users().isAllowed("createCustomer"));
    }
}
