package io.github.josemodi97.sageactive4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.exception.SageActive4jConfigurationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SageActive4jConfigTest {

    @Test
    void defaults() {
        SageActive4jConfig c = SageActive4jConfig.builder().subscriptionKey("k").build();
        assertEquals(Region.FR, c.getRegion());
        assertEquals("https://api.fr.active.sage.com/graphql", c.getGraphQLUrl());
        assertEquals("x-api-key", c.getSubscriptionKeyHeader());
        assertEquals("RDSA WDSA offline_access", c.getScopes());
        assertEquals("https://sbcauth.sage.fr/connect/token", c.getTokenUrl());
        assertEquals(3, c.getMaxRetryAttempts());
        assertEquals(30_000, c.getConnectTimeoutMillis());
        assertEquals(60_000, c.getReadTimeoutMillis());
        assertNull(c.getOrganizationId());
    }

    @Test
    void regionsResolveTheirGateways() {
        assertEquals("https://api.es.active.sage.com/graphql", url(Region.ES));
        assertEquals("https://api.de.active.sage.com/graphql", url(Region.DE));
        // Portugal is served by the Spanish gateway.
        assertEquals("https://api.es.active.sage.com/graphql", url(Region.PT));
    }

    private static String url(Region region) {
        return SageActive4jConfig.builder().region(region).subscriptionKey("k").build().getGraphQLUrl();
    }

    @Test
    void baseUrlOverridesRegionAndToleratesGraphQLSuffix() {
        for (String base : new String[] {"http://localhost:8080", "http://localhost:8080/", "http://localhost:8080/graphql",
                "http://localhost:8080/GraphQL/"}) {
            assertEquals("http://localhost:8080/graphql",
                    SageActive4jConfig.builder().region(Region.DE).baseUrl(base).subscriptionKey("k").build().getGraphQLUrl(),
                    base);
        }
    }

    @Test
    void blankStringsCountAsUnset() {
        SageActive4jConfig c = SageActive4jConfig.builder().subscriptionKey("k").organizationId("  ").scopes(" ").build();
        assertNull(c.getOrganizationId());
        assertEquals(SageActive4jConfig.DEFAULT_SCOPES, c.getScopes());
    }

    @Test
    void zeroRetriesIsAllowed() {
        assertEquals(0, SageActive4jConfig.builder().maxRetryAttempts(0).build().getMaxRetryAttempts());
    }

    @Test
    void validateRequiresSubscriptionKey() {
        SageActive4jConfigurationException e = assertThrows(SageActive4jConfigurationException.class,
                () -> SageActive4jConfig.builder().build().validate());
        assertTrue(e.getMessage().contains("subscriptionKey"));
        assertTrue(e.getMessage().contains("SAGEACTIVE4J_SUBSCRIPTION_KEY"));
    }

    @Test
    void validateRequiresClientIdWithRefreshToken() {
        SageActive4jConfigurationException e = assertThrows(SageActive4jConfigurationException.class,
                () -> SageActive4jConfig.builder().subscriptionKey("k").refreshToken("r").build().validate());
        assertTrue(e.getMessage().contains("clientId"));
    }

    @Test
    void readsEnvironment() {
        Map<String, String> env = new HashMap<String, String>();
        env.put("SAGEACTIVE4J_REGION", "de");
        env.put("SAGEACTIVE4J_SUBSCRIPTION_KEY", "sub");
        env.put("SAGEACTIVE4J_ORGANIZATION_ID", "org-1");
        env.put("SAGEACTIVE4J_CLIENT_ID", "cid");
        env.put("SAGEACTIVE4J_CLIENT_SECRET", "secret");
        env.put("SAGEACTIVE4J_REFRESH_TOKEN", "rt");
        env.put("SAGEACTIVE4J_MAX_RETRY_ATTEMPTS", "5");
        env.put("SAGEACTIVE4J_READ_TIMEOUT_MILLIS", "1234");

        SageActive4jConfig c = SageActive4jConfig.fromEnvironment(env);
        assertEquals(Region.DE, c.getRegion());
        assertEquals("sub", c.getSubscriptionKey());
        assertEquals("org-1", c.getOrganizationId());
        assertEquals("rt", c.getRefreshToken());
        assertEquals(5, c.getMaxRetryAttempts());
        assertEquals(1234, c.getReadTimeoutMillis());
        c.validate();
    }

    @Test
    void badEnvironmentValuesAreReportedByName() {
        Map<String, String> env = new HashMap<String, String>();
        env.put("SAGEACTIVE4J_REGION", "UK");
        SageActive4jConfigurationException e = assertThrows(SageActive4jConfigurationException.class,
                () -> SageActive4jConfig.fromEnvironment(env));
        assertTrue(e.getMessage().contains("SAGEACTIVE4J_REGION"));

        env.put("SAGEACTIVE4J_REGION", "FR");
        env.put("SAGEACTIVE4J_MAX_RETRY_ATTEMPTS", "lots");
        e = assertThrows(SageActive4jConfigurationException.class, () -> SageActive4jConfig.fromEnvironment(env));
        assertTrue(e.getMessage().contains("SAGEACTIVE4J_MAX_RETRY_ATTEMPTS"));
    }

    @Test
    void toStringMasksEverySecret() {
        String s = SageActive4jConfig.builder().subscriptionKey("SUB-SECRET").clientSecret("CLIENT-SECRET")
                .accessToken("ACCESS-SECRET").refreshToken("REFRESH-SECRET").clientId("cid").build().toString();
        assertFalse(s.contains("SECRET"), s);
        assertTrue(s.contains("cid"));
    }

    @Test
    void toBuilderCopiesEverything() {
        SageActive4jConfig original = SageActive4jConfig.builder().region(Region.ES).subscriptionKey("k")
                .organizationId("o").clientId("c").maxRetryAttempts(0).build();
        SageActive4jConfig copy = original.toBuilder().organizationId("o2").build();
        assertEquals(Region.ES, copy.getRegion());
        assertEquals("k", copy.getSubscriptionKey());
        assertEquals("o2", copy.getOrganizationId());
        assertEquals(0, copy.getMaxRetryAttempts());
    }
}
