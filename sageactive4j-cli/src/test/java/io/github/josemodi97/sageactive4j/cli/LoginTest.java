package io.github.josemodi97.sageactive4j.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

/**
 * The browser is simulated: the BrowserOpener receives the sign-in URL and
 * plays SBC Auth's redirect back to the CLI's loopback listener.
 */
class LoginTest extends CliTestSupport {

    private final HttpClient http = HttpClient.newHttpClient();
    private final List<Integer> callbackStatuses = new CopyOnWriteArrayList<>();

    private String callbackUrl(String authUrl, String state, String extra) throws Exception {
        Map<String, String> params = LoopbackLogin.query(URI.create(authUrl).getRawQuery());
        String redirect = params.get("redirect_uri");
        return redirect + "?state=" + (state != null ? state : params.get("state")) + extra;
    }

    private void callBack(String url) {
        try {
            callbackStatuses.add(http.send(HttpRequest.newBuilder(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Plays the browser in the background (the CLI blocks waiting for the callback). */
    private void browserDoes(java.util.function.Consumer<String> behaviour) {
        browser = url -> {
            new Thread(() -> behaviour.accept(url)).start();
            return true;
        };
    }

    @Test
    void signInThroughTheLoopbackListener() throws Exception {
        profile("p", "client-id", "cid", "redirect-uri", "http://127.0.0.1:8765/callback");
        String[] sentUrl = new String[1];
        browserDoes(url -> {
            sentUrl[0] = url;
            try {
                callBack(callbackUrl(url, "forged", "&code=evil"));   // a stray/forged callback first
                callBack(callbackUrl(url, null, "&code=real-code"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        Run r = run("login", "--port", "0", "--timeout", "20");

        assertEquals(0, r.exitCode, r.toString());
        assertTrue(r.out.contains("Signed in."), r.out);
        assertEquals(List.of(400, 200), callbackStatuses);
        assertEquals(1, sage.tokenRequests.size(), "the forged callback must not be exchanged");
        String form = URLDecoder.decode(sage.tokenRequests.get(0), StandardCharsets.UTF_8);
        assertTrue(form.contains("code=real-code"), form);
        assertTrue(form.matches(".*code_verifier=[A-Za-z0-9_-]{43}.*"), form);
        assertTrue(form.matches(".*redirect_uri=http://127\\.0\\.0\\.1:\\d+/callback.*"), form);
        assertTrue(sentUrl[0].contains("code_challenge_method=S256"));
        assertTrue(Files.readString(home.resolve("tokens-p.properties")).contains("signed-in-token"));
    }

    @Test
    void theSignedInTokenIsUsedAfterwards() throws Exception {
        profile("p", "client-id", "cid");
        browserDoes(url -> {
            try {
                callBack(callbackUrl(url, null, "&code=c"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        assertEquals(0, run("login", "--port", "0").exitCode);

        sage.on("userProfile", "{\"userProfile\":{\"fullName\":\"Ada\"}}");
        assertEquals(0, run("query", "--no-org", "{ userProfile { fullName } }").exitCode);
        assertEquals("Bearer signed-in-token", sage.graphql.get(0).authorization);
    }

    @Test
    void userDeclining() throws Exception {
        profile("p", "client-id", "cid");
        browserDoes(url -> {
            try {
                callBack(callbackUrl(url, null, "&error=access_denied&error_description=User+cancelled"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        Run r = run("login", "--port", "0", "--timeout", "20");
        assertEquals(1, r.exitCode);
        assertTrue(r.err.contains("access_denied - User cancelled"), r.err);
        assertTrue(sage.tokenRequests.isEmpty());
    }

    @Test
    void timesOutWhenNobodySignsIn() {
        profile("p", "client-id", "cid");
        Run r = run("login", "--port", "0", "--timeout", "1", "--no-browser");
        assertEquals(1, r.exitCode);
        assertTrue(r.out.contains("Open this URL to sign in"), r.out);
        assertTrue(r.err.contains("timed out"), r.err);
    }

    @Test
    void refusesANonLoopbackRedirect() {
        profile("p", "client-id", "cid", "redirect-uri", "https://app.example.com/callback");
        Run r = run("login", "--no-browser");
        assertEquals(1, r.exitCode);
        assertTrue(r.err.contains("loopback redirect URI"), r.err);
    }

    @Test
    void needsAClientId() {
        profile("p");
        Run r = run("login", "--no-browser");
        assertEquals(1, r.exitCode);
        assertTrue(r.err.contains("client id"), r.err);
    }

    @Test
    void logoutRevokesAndDeletes() throws Exception {
        profile("p", "client-id", "cid", "revocation-url", "unused");
        Files.writeString(home.resolve("tokens-p.properties"), "access_token=a\nrefresh_token=the-refresh\n");
        env.put("SAGEACTIVE4J_REVOCATION_URL", sage.url() + "/connect/revocation");

        Run r = run("logout");

        assertEquals(0, r.exitCode, r.toString());
        assertFalse(Files.exists(home.resolve("tokens-p.properties")));
        assertTrue(sage.revocations.get(0).contains("token=the-refresh"), sage.revocations.toString());
        assertTrue(run("logout").out.contains("Not signed in"));
    }
}
