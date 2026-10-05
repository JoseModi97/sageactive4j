package io.github.josemodi97.sageactive4j.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import io.github.josemodi97.sageactive4j.exception.SageActive4jAuthException;
import io.github.josemodi97.sageactive4j.testsupport.MutableClock;
import io.github.josemodi97.sageactive4j.testsupport.StubServer;
import io.github.josemodi97.sageactive4j.testsupport.StubServer.Recorded;
import io.github.josemodi97.sageactive4j.testsupport.StubServer.Reply;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SageAuthClientTest {

    private static final String TOKEN = "/connect/token";
    private static final String REVOKE = "/connect/revocation";

    private StubServer server;
    private final MutableClock clock = new MutableClock(1_790_000_000_000L);

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
                .subscriptionKey("sub")
                .clientId("client-id")
                .clientSecret("client-secret")
                .redirectUri("https://app.example.com/callback")
                .authUrl(server.url() + "/connect/authorize")
                .tokenUrl(server.url() + TOKEN)
                .revocationUrl(server.url() + REVOKE);
    }

    private static Reply token(String access, String refresh, int expiresIn) {
        return Reply.json("{\"access_token\":\"" + access + "\","
                + (refresh == null ? "" : "\"refresh_token\":\"" + refresh + "\",")
                + "\"token_type\":\"Bearer\",\"expires_in\":" + expiresIn + ",\"scope\":\"RDSA WDSA offline_access\"}");
    }

    @Test
    void buildsAnAuthorizationUrlWithPkce() {
        SageAuthClient auth = new SageAuthClient(config().build(), clock);
        PkceChallenge pkce = PkceChallenge.of("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk");

        String url = auth.buildAuthorizationUrl("state-123", null, pkce);

        assertTrue(url.startsWith(server.url() + "/connect/authorize?response_type=code&"), url);
        assertTrue(url.contains("client_id=client-id"));
        assertTrue(url.contains("redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback"));
        assertTrue(url.contains("scope=RDSA%20WDSA%20offline_access"));
        assertTrue(url.contains("state=state-123"));
        assertTrue(url.contains("code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"));
        assertTrue(url.contains("code_challenge_method=S256"));
    }

    @Test
    void authorizationUrlRequiresState() {
        SageAuthClient auth = new SageAuthClient(config().build(), clock);
        assertThrows(IllegalArgumentException.class, () -> auth.buildAuthorizationUrl(" "));
    }

    @Test
    void exchangesACodeAndSavesTheToken() {
        InMemoryTokenStore store = new InMemoryTokenStore();
        SageAuthClient auth = new SageAuthClient(config().tokenStore(store).build(), clock);
        server.enqueue(TOKEN, token("access-1", "refresh-1", 28_800));

        SageToken token = auth.exchangeCode("the-code", null, "verifier-xyz");

        assertEquals("access-1", token.getAccessToken());
        assertEquals(Long.valueOf(clock.millis() + 28_800_000L), token.getExpiresAtEpochMillis());
        assertEquals("refresh-1", store.load().getRefreshToken());

        Recorded request = server.requests(TOKEN).get(0);
        assertEquals("POST", request.method);
        assertEquals("application/x-www-form-urlencoded", request.header("Content-Type"));
        Map<String, String> form = request.form();
        assertEquals("authorization_code", form.get("grant_type"));
        assertEquals("the-code", form.get("code"));
        assertEquals("https://app.example.com/callback", form.get("redirect_uri"));
        assertEquals("client-id", form.get("client_id"));
        assertEquals("client-secret", form.get("client_secret"));
        assertEquals("verifier-xyz", form.get("code_verifier"));
    }

    @Test
    void publicClientsSendNoSecret() {
        SageAuthClient auth = new SageAuthClient(config().clientSecret(null).build(), clock);
        server.enqueue(TOKEN, token("a", "r", 3600));
        auth.exchangeCode("code", null, "v");
        assertFalse(server.requests(TOKEN).get(0).form().containsKey("client_secret"));
    }

    @Test
    void refreshesLazilyFromAConfiguredRefreshToken() {
        SageAuthClient auth = new SageAuthClient(config().refreshToken("seed-refresh").build(), clock);
        assertTrue(server.requests().isEmpty(), "construction must not touch the network");

        server.enqueue(TOKEN, token("access-1", "refresh-2", 3600));
        assertEquals("access-1", auth.getValidAccessToken());
        assertEquals("access-1", auth.getValidAccessToken()); // cached

        assertEquals(1, server.requests(TOKEN).size());
        Map<String, String> form = server.requests(TOKEN).get(0).form();
        assertEquals("refresh_token", form.get("grant_type"));
        assertEquals("seed-refresh", form.get("refresh_token"));
        assertEquals("refresh-2", auth.currentToken().getRefreshToken(), "rotated refresh token must be kept");
    }

    @Test
    void refreshesWithinTheSafetyMarginBeforeExpiry() {
        SageAuthClient auth = new SageAuthClient(config().refreshToken("r0").build(), clock);
        server.enqueue(TOKEN, token("access-1", "r1", 3600));
        server.enqueue(TOKEN, token("access-2", "r2", 3600));

        assertEquals("access-1", auth.getValidAccessToken());
        clock.advanceSeconds(3600 - 61);
        assertEquals("access-1", auth.getValidAccessToken());
        clock.advanceSeconds(2); // now inside the 60 s margin
        assertEquals("access-2", auth.getValidAccessToken());
        assertEquals("r1", server.requests(TOKEN).get(1).form().get("refresh_token"));
    }

    @Test
    void keepsTheOldRefreshTokenWhenNotRotated() {
        SageAuthClient auth = new SageAuthClient(config().refreshToken("keep-me").build(), clock);
        server.enqueue(TOKEN, token("access-1", null, 3600));
        auth.getValidAccessToken();
        assertEquals("keep-me", auth.currentToken().getRefreshToken());
    }

    @Test
    void concurrentCallersShareOneRefresh() throws Exception {
        SageAuthClient auth = new SageAuthClient(config().refreshToken("r0").build(), clock);
        server.enqueue(TOKEN, token("shared", "r1", 3600).delay(200));

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<Future<String>>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                go.await();
                return auth.getValidAccessToken();
            }));
        }
        go.countDown();
        for (Future<String> result : results) {
            assertEquals("shared", result.get());
        }
        pool.shutdown();
        assertEquals(1, server.requests(TOKEN).size());
    }

    @Test
    void storedTokensTakePrecedenceOverConfiguredOnes() {
        InMemoryTokenStore store = new InMemoryTokenStore();
        store.save(new SageToken("stored-access", "stored-refresh", "Bearer", null, clock.millis() + 3_600_000L));
        SageAuthClient auth = new SageAuthClient(config().tokenStore(store).refreshToken("stale-config-refresh").build(), clock);
        assertEquals("stored-access", auth.getValidAccessToken());
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void staticAccessTokenWithUnknownExpiryIsUsedAsIs() {
        SageAuthClient auth = new SageAuthClient(config().accessToken("static").build(), clock);
        clock.advanceSeconds(100_000);
        assertEquals("static", auth.getValidAccessToken());
        assertFalse(auth.canRefresh());
    }

    @Test
    void noTokenAtAllExplainsHowToSignIn() {
        SageAuthClient auth = new SageAuthClient(config().build(), clock);
        SageActive4jAuthException e = assertThrows(SageActive4jAuthException.class, auth::getValidAccessToken);
        assertTrue(e.getMessage().contains("exchangeCode"));
    }

    @Test
    void invalidGrantIsReportedWithItsOAuthErrorCode() {
        SageAuthClient auth = new SageAuthClient(config().refreshToken("revoked").build(), clock);
        server.enqueue(TOKEN, Reply.status(400).body("{\"error\":\"invalid_grant\",\"error_description\":\"token expired\"}"));

        SageActive4jAuthException e = assertThrows(SageActive4jAuthException.class, auth::getValidAccessToken);
        assertEquals("invalid_grant", e.getOAuthError());
        assertEquals(400, e.getHttpStatus());
        assertTrue(e.getMessage().contains("sign in again"), e.getMessage());
    }

    @Test
    void unreadableSuccessBodyDoesNotLeakIntoTheException() {
        SageAuthClient auth = new SageAuthClient(config().refreshToken("r").build(), clock);
        server.enqueue(TOKEN, Reply.status(200).body("access_token=SECRET-LOOKING-VALUE"));
        SageActive4jAuthException e = assertThrows(SageActive4jAuthException.class, auth::getValidAccessToken);
        assertFalse(e.getMessage().contains("SECRET"));
        assertNull(e.getResponseBody());
    }

    @Test
    void forceRefreshSkipsWhenAnotherThreadAlreadyRefreshed() {
        SageAuthClient auth = new SageAuthClient(config().refreshToken("r0").build(), clock);
        server.enqueue(TOKEN, token("access-1", "r1", 3600));
        server.enqueue(TOKEN, token("access-2", "r2", 3600));

        String first = auth.getValidAccessToken();
        assertEquals("access-2", auth.forceRefresh(first));
        // A second caller still holding the old rejected token must not trigger another refresh.
        assertEquals("access-2", auth.forceRefresh(first));
        assertEquals(2, server.requests(TOKEN).size());
    }

    @Test
    void revokeRevokesTheRefreshTokenAndClearsTheStore() {
        InMemoryTokenStore store = new InMemoryTokenStore();
        store.save(new SageToken("a", "the-refresh", "Bearer", null, null));
        SageAuthClient auth = new SageAuthClient(config().tokenStore(store).build(), clock);
        server.enqueue(REVOKE, Reply.status(200));

        auth.revoke();

        Map<String, String> form = server.requests(REVOKE).get(0).form();
        assertEquals("the-refresh", form.get("token"));
        assertEquals("refresh_token", form.get("token_type_hint"));
        assertNull(store.load());
        assertNull(auth.currentToken());
    }
}
