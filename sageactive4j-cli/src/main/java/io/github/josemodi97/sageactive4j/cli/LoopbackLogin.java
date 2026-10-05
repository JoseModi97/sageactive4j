package io.github.josemodi97.sageactive4j.cli;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import io.github.josemodi97.sageactive4j.auth.PkceChallenge;
import io.github.josemodi97.sageactive4j.auth.SageAuthClient;
import io.github.josemodi97.sageactive4j.auth.SageToken;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Desktop OAuth sign-in (RFC 8252): a one-shot HTTP listener on the loopback
 * interface receives the authorization code; PKCE protects the exchange and
 * a single-use {@code state} rejects stray or forged callbacks (which are
 * answered 400 while the real one is still awaited).
 */
final class LoopbackLogin {

    static final int DEFAULT_TIMEOUT_SECONDS = 300;

    private LoopbackLogin() {
    }

    /**
     * @param portOverride listen on this port instead of the redirect URI's (0 = any free port)
     * @return the token set, already saved to the profile's token file
     */
    static SageToken signIn(SageActive4jCli root, Integer portOverride, int timeoutSeconds, boolean openBrowser) {
        SageActive4jConfig config = root.config();
        if (config.getClientId() == null) {
            throw new CliException("Signing in needs an OAuth client id: run 'sageactive4j init' or pass --client-id.");
        }
        URI redirect = URI.create(config.getRedirectUri() != null ? config.getRedirectUri() : InitCommand.DEFAULT_REDIRECT_URI);
        if (!"http".equals(redirect.getScheme()) || !isLoopback(redirect.getHost())) {
            throw new CliException("The CLI signs in through a loopback redirect URI such as "
                    + InitCommand.DEFAULT_REDIRECT_URI + "; this profile has " + redirect);
        }
        int port = portOverride != null ? portOverride : (redirect.getPort() > 0 ? redirect.getPort() : 80);
        String path = redirect.getPath() == null || redirect.getPath().isEmpty() ? "/" : redirect.getPath();

        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        final String state = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        PkceChallenge pkce = PkceChallenge.generate();
        final CompletableFuture<String> code = new CompletableFuture<String>();

        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(redirect.getHost()), port), 0);
        } catch (IOException e) {
            throw new CliException("Could not listen on " + redirect.getHost() + ":" + port + " for the sign-in callback ("
                    + e.getMessage() + "). Is another login running?");
        }
        server.createContext(path, exchange -> handle(exchange, state, code));
        server.start();
        try {
            String actualRedirect = new URI("http", null, redirect.getHost(), server.getAddress().getPort(), path, null, null).toString();
            SageAuthClient auth = new SageAuthClient(config);
            String url = auth.buildAuthorizationUrl(state, actualRedirect, pkce);

            CliContext context = root.context;
            boolean opened = openBrowser && context.browser.open(url);
            context.out.println(opened ? "Opened your browser to sign in to Sage Active." : "Open this URL to sign in to Sage Active:");
            context.out.println("  " + url);
            context.out.println("Waiting for the sign-in to complete (up to " + timeoutSeconds + " s)...");
            context.out.flush();

            String authorizationCode;
            try {
                authorizationCode = code.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new CliException("Sign-in timed out after " + timeoutSeconds + " s; run 'sageactive4j login' again.");
            } catch (ExecutionException e) {
                throw new CliException(e.getCause().getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CliException("Sign-in was interrupted.");
            }
            SageToken token = auth.exchangeCode(authorizationCode, actualRedirect, pkce.getVerifier());
            context.out.println("Signed in. Tokens saved to " + root.configFile().tokenFile(root.profileName(root.configFile())));
            return token;
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(e);
        } finally {
            server.stop(0);
        }
    }

    private static void handle(HttpExchange exchange, String expectedState, CompletableFuture<String> code) throws IOException {
        try {
            Map<String, String> params = query(exchange.getRequestURI().getRawQuery());
            String error = params.get("error");
            String state = params.get("state");
            boolean stateOk = state != null && MessageDigest.isEqual(
                    expectedState.getBytes(StandardCharsets.UTF_8), state.getBytes(StandardCharsets.UTF_8));
            if (!stateOk || code.isDone()) {
                respond(exchange, 400, "This sign-in link is not the one this terminal is waiting for.");
                return;
            }
            // Answer the browser before releasing the waiting thread, which
            // stops this server as soon as it resumes.
            if (error != null) {
                respond(exchange, 400, "Sign-in failed (" + error + "). You can close this tab.");
                code.completeExceptionally(new CliException("Sign-in was refused by SBC Auth: " + error
                        + (params.get("error_description") == null ? "" : " - " + params.get("error_description"))));
                return;
            }
            String value = params.get("code");
            if (value == null || value.isEmpty()) {
                respond(exchange, 400, "The sign-in response had no authorization code.");
                return;
            }
            respond(exchange, 200, "Signed in to Sage Active. You can close this tab and return to the terminal.");
            code.complete(value);
        } finally {
            exchange.close();
        }
    }

    private static void respond(HttpExchange exchange, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    static Map<String, String> query(String rawQuery) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<String, String>();
        if (rawQuery == null) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            String key = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), "UTF-8");
            String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
            params.put(key, value);
        }
        return params;
    }

    static boolean isLoopback(String host) {
        return "127.0.0.1".equals(host) || "localhost".equals(host) || "[::1]".equals(host) || "::1".equals(host);
    }
}
