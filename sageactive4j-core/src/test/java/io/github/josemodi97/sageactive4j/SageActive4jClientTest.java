package io.github.josemodi97.sageactive4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.exception.SageActive4jApiException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jAuthException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jConfigurationException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jGraphQLException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jRateLimitException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jTransportException;
import io.github.josemodi97.sageactive4j.graphql.GraphQLRequest;
import io.github.josemodi97.sageactive4j.graphql.GraphQLResponse;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import io.github.josemodi97.sageactive4j.testsupport.MutableClock;
import io.github.josemodi97.sageactive4j.testsupport.RecordingSleeper;
import io.github.josemodi97.sageactive4j.testsupport.StubServer;
import io.github.josemodi97.sageactive4j.testsupport.StubServer.Recorded;
import io.github.josemodi97.sageactive4j.testsupport.StubServer.Reply;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SageActive4jClientTest {

    private static final String GQL = "/graphql";
    private static final String TOKEN = "/connect/token";
    private static final String PROFILE = "{ userProfile { fullName } }";

    private StubServer server;
    private final MutableClock clock = new MutableClock(1_790_000_000_000L);
    private final RecordingSleeper sleeper = new RecordingSleeper();

    @BeforeEach
    void start() throws Exception {
        server = new StubServer();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    private SageActive4jConfig.Builder config() {
        return SageActive4jConfig.builder()
                .baseUrl(server.url())
                .tokenUrl(server.url() + TOKEN)
                .subscriptionKey("sub-key")
                .organizationId("org-1")
                .clientId("cid")
                .clientSecret("secret")
                .refreshToken("refresh-0");
    }

    private SageActive4jClient client(SageActive4jConfig.Builder builder) {
        return new SageActive4jClient(builder.build(), clock, sleeper);
    }

    private static Reply token(String access) {
        return Reply.json("{\"access_token\":\"" + access + "\",\"refresh_token\":\"r-" + access
                + "\",\"expires_in\":28800,\"token_type\":\"Bearer\"}");
    }

    private static Reply profile(String name) {
        return Reply.json("{\"data\":{\"userProfile\":{\"fullName\":\"" + name + "\"}}}");
    }

    @Test
    void sendsTheGraphQLRequestWithEveryRequiredHeader() {
        server.enqueue(TOKEN, token("access-1"));
        server.enqueue(GQL, profile("Ada Lovelace"));
        SageActive4jClient sage = client(config().countryCode("fr"));

        Map<String, Object> vars = new LinkedHashMap<String, Object>();
        vars.put("first", 10);
        vars.put("amount", new BigDecimal("12.30"));
        Map<String, Object> data = sage.query(PROFILE, vars);

        assertEquals("Ada Lovelace", JsonReader.getString(data, "userProfile", "fullName"));
        Recorded request = server.requests(GQL).get(0);
        assertEquals("POST", request.method);
        assertEquals("Bearer access-1", request.header("Authorization"));
        assertEquals("sub-key", request.header("x-api-key"));
        assertEquals("org-1", request.header("X-OrganizationId"));
        assertEquals("fr", request.header("X-Country-Code"));
        assertTrue(request.header("Content-Type").startsWith("application/json"));
        assertTrue(request.header("User-Agent").startsWith("sageactive4j/"), request.header("User-Agent"));

        Map<String, Object> body = JsonReader.parseObject(request.body);
        assertEquals(PROFILE, body.get("query"));
        assertEquals(10L, JsonReader.getLong(body, "variables", "first"));
        assertEquals("12.30", JsonReader.getString(body, "variables", "amount"));
    }

    @Test
    void customSubscriptionKeyHeader() {
        server.enqueue(TOKEN, token("a"));
        server.enqueue(GQL, profile("x"));
        client(config().subscriptionKeyHeader("Ocp-Apim-Subscription-Key")).query(PROFILE);
        Recorded request = server.requests(GQL).get(0);
        assertEquals("sub-key", request.header("Ocp-Apim-Subscription-Key"));
        assertNull(request.header("x-api-key"));
    }

    @Test
    void withOrganizationChangesOnlyTheTenantHeaderAndSharesTheToken() {
        server.enqueue(TOKEN, token("shared"));
        server.always(GQL, profile("x"));
        SageActive4jClient sage = client(config());

        sage.withOrganization("org-2").query(PROFILE);
        sage.withOrganization(null).query(PROFILE);
        sage.query(PROFILE);

        assertEquals("org-2", server.requests(GQL).get(0).header("X-OrganizationId"));
        assertNull(server.requests(GQL).get(1).header("X-OrganizationId"));
        assertEquals("org-1", server.requests(GQL).get(2).header("X-OrganizationId"));
        assertEquals(1, server.requests(TOKEN).size(), "views must share one token cache");
    }

    @Test
    void graphQLErrorsAreReturnedByExecuteAndThrownByQuery() {
        String body = "{\"errors\":[{\"message\":\"Not allowed\",\"path\":[\"salesInvoices\"],"
                + "\"extensions\":{\"code\":\"AUTH_NOT_AUTHORIZED\"}},{\"message\":\"second\"}],"
                + "\"data\":{\"userProfile\":{\"fullName\":\"partial\"}}}";
        server.enqueue(TOKEN, token("a"));
        server.always(GQL, Reply.json(body));
        SageActive4jClient sage = client(config());

        GraphQLResponse response = sage.execute(new GraphQLRequest(PROFILE));
        assertTrue(response.hasErrors());
        assertEquals("AUTH_NOT_AUTHORIZED", response.getErrors().get(0).getCode());
        assertEquals(Arrays.<Object>asList("salesInvoices"), response.getErrors().get(0).getPath());

        SageActive4jGraphQLException e = assertThrows(SageActive4jGraphQLException.class, () -> sage.query(PROFILE));
        assertEquals("AUTH_NOT_AUTHORIZED", e.getErrorCode());
        assertEquals(2, e.getErrors().size());
        assertEquals("partial", JsonReader.getString(e.getData(), "userProfile", "fullName"));
        assertTrue(e.getMessage().contains("Not allowed") && e.getMessage().contains("1 more"), e.getMessage());
    }

    @Test
    void graphQLErrorsOnA4xxAreStillGraphQLErrors() {
        server.enqueue(TOKEN, token("a"));
        server.enqueue(GQL, Reply.status(400).header("Content-Type", "application/json")
                .body("{\"errors\":[{\"message\":\"Syntax error\",\"extensions\":{\"code\":\"HC0011\"}}]}"));
        SageActive4jGraphQLException e = assertThrows(SageActive4jGraphQLException.class,
                () -> client(config()).query("{ broken"));
        assertEquals(400, e.getHttpStatus());
        assertEquals("HC0011", e.getErrorCode());
    }

    @Test
    void retriesOn429HonouringRetryAfter() {
        server.enqueue(TOKEN, token("a"));
        server.enqueue(GQL, Reply.status(429).header("Retry-After", "2"));
        server.enqueue(GQL, Reply.status(429).header("Retry-After", "1"));
        server.enqueue(GQL, profile("eventually"));

        Map<String, Object> data = client(config()).query(PROFILE);

        assertEquals("eventually", JsonReader.getString(data, "userProfile", "fullName"));
        assertEquals(3, server.requests(GQL).size());
        assertEquals(Arrays.asList(2_000L, 1_000L), sleeper.pauses());
    }

    @Test
    void givesUpAfterMaxRetries() {
        server.enqueue(TOKEN, token("a"));
        server.always(GQL, Reply.status(429).body("slow down"));

        SageActive4jRateLimitException e = assertThrows(SageActive4jRateLimitException.class,
                () -> client(config().maxRetryAttempts(2)).query(PROFILE));

        assertEquals(3, e.getAttempts());
        assertEquals(3, server.requests(GQL).size());
        assertEquals(429, e.getHttpStatus());
        assertEquals(2, sleeper.pauses().size());
    }

    @Test
    void zeroRetriesFailsOnTheFirst429() {
        server.enqueue(TOKEN, token("a"));
        server.always(GQL, Reply.status(429));
        assertThrows(SageActive4jRateLimitException.class, () -> client(config().maxRetryAttempts(0)).query(PROFILE));
        assertEquals(1, server.requests(GQL).size());
        assertTrue(sleeper.pauses().isEmpty());
    }

    @Test
    void a401TriggersOneRefreshAndReplay() {
        server.enqueue(TOKEN, token("old"));
        server.enqueue(TOKEN, token("new"));
        server.enqueue(GQL, Reply.status(401));
        server.enqueue(GQL, profile("after refresh"));

        Map<String, Object> data = client(config()).query(PROFILE);

        assertEquals("after refresh", JsonReader.getString(data, "userProfile", "fullName"));
        assertEquals("Bearer old", server.requests(GQL).get(0).header("Authorization"));
        assertEquals("Bearer new", server.requests(GQL).get(1).header("Authorization"));
        assertEquals("r-old", server.requests(TOKEN).get(1).form().get("refresh_token"));
    }

    @Test
    void aSecond401DoesNotLoop() {
        server.enqueue(TOKEN, token("old"));
        server.enqueue(TOKEN, token("new"));
        server.always(GQL, Reply.status(401).body("{\"message\":\"no access\"}"));

        SageActive4jAuthException e = assertThrows(SageActive4jAuthException.class, () -> client(config()).query(PROFILE));
        assertEquals(401, e.getHttpStatus());
        assertEquals(2, server.requests(GQL).size());
        assertEquals(2, server.requests(TOKEN).size());
    }

    @Test
    void a401WithoutRefreshTokenFailsClearly() {
        server.always(GQL, Reply.status(401));
        SageActive4jClient sage = client(config().refreshToken(null).accessToken("static-token"));
        SageActive4jAuthException e = assertThrows(SageActive4jAuthException.class, () -> sage.query(PROFILE));
        assertTrue(e.getMessage().contains("no refresh token"), e.getMessage());
        assertEquals(1, server.requests(GQL).size());
    }

    @Test
    void nonGraphQLErrorBodiesBecomeApiExceptions() {
        server.enqueue(TOKEN, token("a"));
        server.always(GQL, Reply.status(503).body("<html>Service Unavailable</html>"));
        SageActive4jApiException e = assertThrows(SageActive4jApiException.class, () -> client(config()).query(PROFILE));
        assertEquals(503, e.getHttpStatus());
        assertEquals("<html>Service Unavailable</html>", e.getResponseBody());
        assertTrue(e.getMessage().contains("server error"), e.getMessage());
    }

    @Test
    void forbiddenGetsAnActionableHint() {
        server.enqueue(TOKEN, token("a"));
        server.always(GQL, Reply.status(403).body("{\"statusCode\":403}"));
        SageActive4jApiException e = assertThrows(SageActive4jApiException.class, () -> client(config()).query(PROFILE));
        assertTrue(e.getMessage().contains("subscription key"), e.getMessage());
    }

    @Test
    void a2xxThatIsNotGraphQLIsRejected() {
        server.enqueue(TOKEN, token("a"));
        server.always(GQL, Reply.json("{\"hello\":\"world\"}"));
        assertThrows(SageActive4jApiException.class, () -> client(config()).query(PROFILE));
    }

    @Test
    void connectionFailuresAreTransportExceptions() {
        String deadUrl = server.url();
        server.close();
        SageActive4jClient sage = new SageActive4jClient(SageActive4jConfig.builder()
                .baseUrl(deadUrl).subscriptionKey("k").accessToken("t").connectTimeoutMillis(2_000).build(), clock, sleeper);
        assertThrows(SageActive4jTransportException.class, () -> sage.query(PROFILE));
    }

    @Test
    void mapperConvertsData() {
        server.enqueue(TOKEN, token("a"));
        server.enqueue(GQL, profile("Grace Hopper"));
        String name = client(config()).query(PROFILE, Collections.<String, Object>emptyMap(),
                data -> JsonReader.getString(data, "userProfile", "fullName"));
        assertEquals("Grace Hopper", name);
    }

    @Test
    void constructorValidatesTheConfig() {
        assertThrows(SageActive4jConfigurationException.class,
                () -> new SageActive4jClient(SageActive4jConfig.builder().build()));
    }

    @Test
    void toStringDoesNotLeakSecrets() {
        String s = client(config().accessToken("ACCESS-SECRET")).toString();
        assertTrue(!s.contains("secret") && !s.contains("SECRET") && !s.contains("sub-key") && !s.contains("refresh-0"), s);
    }
}
