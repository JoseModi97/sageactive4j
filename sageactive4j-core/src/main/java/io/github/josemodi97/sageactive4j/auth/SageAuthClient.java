package io.github.josemodi97.sageactive4j.auth;

import io.github.josemodi97.sageactive4j.SageActive4j;
import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import io.github.josemodi97.sageactive4j.exception.SageActive4jAuthException;
import io.github.josemodi97.sageactive4j.internal.HttpRequest;
import io.github.josemodi97.sageactive4j.internal.HttpResponse;
import io.github.josemodi97.sageactive4j.internal.HttpTransport;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth 2.0 against SBC Auth (Sage Business Cloud Auth): authorization URLs,
 * code exchange, refresh, revocation, and a thread-safe cache that hands the
 * GraphQL transport a valid access token.
 *
 * <p>Nothing touches the network or the {@link TokenStore} until the first
 * call that needs a token, so constructing a client (e.g. as a DI bean) is
 * cheap and offline.
 *
 * <p>Token precedence on first use: whatever the {@link TokenStore} holds
 * (it may contain a newer, rotated refresh token), else the
 * {@code accessToken}/{@code refreshToken} from the config.
 */
public final class SageAuthClient {

    private final SageActive4jConfig config;
    private final TokenStore store;
    private final Clock clock;
    private final Object lock = new Object();

    private volatile SageToken current;
    private volatile boolean loaded;

    public SageAuthClient(SageActive4jConfig config) {
        this(config, Clock.systemUTC());
    }

    /** With an explicit clock, for deterministic expiry handling in tests. */
    public SageAuthClient(SageActive4jConfig config, Clock clock) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
        this.store = config.getTokenStore() != null ? config.getTokenStore() : new InMemoryTokenStore();
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    // ---------------------------------------------------------------- authorization code flow

    /** The SBC Auth login URL to send the user's browser to, using the configured redirect URI. */
    public String buildAuthorizationUrl(String state) {
        return buildAuthorizationUrl(state, null, null);
    }

    /**
     * The SBC Auth login URL to send the user's browser to.
     *
     * @param state       an unguessable value you check again on the callback (CSRF protection); required
     * @param redirectUri overrides the configured redirect URI when not {@code null}
     * @param pkce        PKCE challenge for public clients, or {@code null}
     */
    public String buildAuthorizationUrl(String state, String redirectUri, PkceChallenge pkce) {
        if (state == null || state.trim().isEmpty()) {
            throw new IllegalArgumentException("state is required: generate an unguessable value and verify it on the callback");
        }
        requireClientId();
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("response_type", "code");
        params.put("client_id", config.getClientId());
        params.put("redirect_uri", resolveRedirectUri(redirectUri));
        params.put("scope", config.getScopes());
        params.put("state", state);
        if (pkce != null) {
            params.put("code_challenge", pkce.getChallenge());
            params.put("code_challenge_method", pkce.getMethod());
        }
        String base = config.getAuthUrl();
        return base + (base.contains("?") ? "&" : "?") + formEncode(params);
    }

    /** Exchanges an authorization code (confidential client, configured redirect URI). */
    public SageToken exchangeCode(String code) {
        return exchangeCode(code, null, null);
    }

    /**
     * Exchanges an authorization code for tokens and saves them to the store.
     *
     * @param redirectUri  must match the one used in the authorization URL; {@code null} = configured one
     * @param codeVerifier the PKCE verifier, or {@code null} for confidential clients
     */
    public SageToken exchangeCode(String code, String redirectUri, String codeVerifier) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        requireClientId();
        Map<String, String> form = new LinkedHashMap<String, String>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", resolveRedirectUri(redirectUri));
        form.put("client_id", config.getClientId());
        if (config.getClientSecret() != null) {
            form.put("client_secret", config.getClientSecret());
        }
        if (codeVerifier != null) {
            form.put("code_verifier", codeVerifier);
        }
        synchronized (lock) {
            SageToken token = requestToken(form);
            setCurrent(token);
            return token;
        }
    }

    // ---------------------------------------------------------------- token cache

    /**
     * A currently valid access token, refreshing it first if it's missing or
     * expires within the configured safety margin. Concurrent callers share
     * one refresh.
     *
     * @throws SageActive4jAuthException if no token can be obtained
     */
    public String getValidAccessToken() {
        ensureLoaded();
        SageToken token = current;
        if (token != null && !token.isExpiredWithin(config.getTokenExpirySafetyMarginSeconds(), clock.millis())) {
            return token.getAccessToken();
        }
        synchronized (lock) {
            token = current;
            if (token != null && !token.isExpiredWithin(config.getTokenExpirySafetyMarginSeconds(), clock.millis())) {
                return token.getAccessToken();
            }
            if (token != null && token.getRefreshToken() != null) {
                return refreshLocked(token.getRefreshToken()).getAccessToken();
            }
            if (token != null && token.getAccessToken() != null) {
                throw new SageActive4jAuthException("The access token has expired and there is no refresh token to "
                        + "renew it. Sign in again, or request the offline_access scope to receive refresh tokens.");
            }
            throw new SageActive4jAuthException("No Sage Active token available. Sign in with "
                    + "buildAuthorizationUrl(...) and exchangeCode(...), or configure accessToken/refreshToken "
                    + "or a TokenStore that already holds tokens.");
        }
    }

    /**
     * Called after Sage Active rejected {@code rejectedAccessToken} with a
     * 401: refreshes unless another thread already replaced that token, and
     * returns the token to retry with.
     *
     * @throws SageActive4jAuthException if there is no refresh token, or the refresh fails
     */
    public String forceRefresh(String rejectedAccessToken) {
        ensureLoaded();
        synchronized (lock) {
            SageToken token = current;
            if (token != null && token.getAccessToken() != null && rejectedAccessToken != null
                    && !token.getAccessToken().equals(rejectedAccessToken)
                    && !token.isExpiredWithin(config.getTokenExpirySafetyMarginSeconds(), clock.millis())) {
                return token.getAccessToken();
            }
            if (token == null || token.getRefreshToken() == null) {
                throw new SageActive4jAuthException("Sage Active rejected the access token (HTTP 401) and there is "
                        + "no refresh token to renew it. Sign in again.", 401, null, null);
            }
            return refreshLocked(token.getRefreshToken()).getAccessToken();
        }
    }

    /** Refreshes now, regardless of expiry. */
    public SageToken refresh() {
        ensureLoaded();
        synchronized (lock) {
            SageToken token = current;
            if (token == null || token.getRefreshToken() == null) {
                throw new SageActive4jAuthException("There is no refresh token to refresh with. Sign in again.");
            }
            return refreshLocked(token.getRefreshToken());
        }
    }

    /** Whether a refresh token is available. */
    public boolean canRefresh() {
        ensureLoaded();
        SageToken token = current;
        return token != null && token.getRefreshToken() != null;
    }

    /** The current token set, or {@code null}. */
    public SageToken currentToken() {
        ensureLoaded();
        return current;
    }

    /**
     * Revokes the refresh token (or, without one, the access token) at SBC
     * Auth, then clears the store. Safe to call when signed out.
     */
    public void revoke() {
        ensureLoaded();
        synchronized (lock) {
            SageToken token = current;
            String toRevoke = token == null ? null
                    : token.getRefreshToken() != null ? token.getRefreshToken() : token.getAccessToken();
            if (toRevoke != null && config.getClientId() != null) {
                Map<String, String> form = new LinkedHashMap<String, String>();
                form.put("token", toRevoke);
                form.put("token_type_hint", token.getRefreshToken() != null ? "refresh_token" : "access_token");
                form.put("client_id", config.getClientId());
                if (config.getClientSecret() != null) {
                    form.put("client_secret", config.getClientSecret());
                }
                HttpResponse response = postForm(config.getRevocationUrl(), form);
                if (!response.isSuccess()) {
                    throw authError("Token revocation failed", response);
                }
            }
            current = null;
            store.clear();
        }
    }

    // ---------------------------------------------------------------- internals

    private SageToken refreshLocked(String refreshToken) {
        requireClientId();
        Map<String, String> form = new LinkedHashMap<String, String>();
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);
        form.put("client_id", config.getClientId());
        if (config.getClientSecret() != null) {
            form.put("client_secret", config.getClientSecret());
        }
        SageToken token = requestToken(form).withRefreshTokenFallback(refreshToken);
        setCurrent(token);
        return token;
    }

    private SageToken requestToken(Map<String, String> form) {
        long requestedAt = clock.millis();
        HttpResponse response = postForm(config.getTokenUrl(), form);
        if (!response.isSuccess()) {
            throw authError("SBC Auth rejected the token request", response);
        }
        Map<String, Object> json;
        try {
            json = JsonReader.parseObject(response.body());
        } catch (RuntimeException e) {
            // Deliberately no body in the exception: a 2xx token response may carry credentials.
            throw new SageActive4jAuthException("SBC Auth returned an unreadable token response (HTTP "
                    + response.status() + ")", response.status(), null, null);
        }
        SageToken token = SageToken.fromTokenResponse(json, requestedAt);
        if (token.getAccessToken() == null || token.getAccessToken().isEmpty()) {
            throw new SageActive4jAuthException("SBC Auth's token response contained no access_token",
                    response.status(), null, null);
        }
        return token;
    }

    private HttpResponse postForm(String url, Map<String, String> form) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept", "application/json");
        headers.put("User-Agent", SageActive4j.userAgent());
        byte[] body = formEncode(form).getBytes(StandardCharsets.UTF_8);
        return HttpTransport.send(new HttpRequest("POST", url, headers, body),
                config.getConnectTimeoutMillis(), config.getReadTimeoutMillis());
    }

    private static SageActive4jAuthException authError(String what, HttpResponse response) {
        String oauthError = null;
        String description = null;
        try {
            Map<String, Object> json = JsonReader.parseObject(response.body());
            oauthError = JsonReader.getString(json, "error");
            description = JsonReader.getString(json, "error_description");
        } catch (RuntimeException ignored) {
            // not JSON; fall through with the status only
        }
        StringBuilder message = new StringBuilder(what).append(" (HTTP ").append(response.status()).append(')');
        if (oauthError != null) {
            message.append(": ").append(oauthError);
            if (description != null) {
                message.append(" - ").append(description);
            }
        }
        if ("invalid_grant".equals(oauthError)) {
            message.append(". The refresh token or code is expired, revoked, or already used; sign in again.");
        }
        return new SageActive4jAuthException(message.toString(), response.status(), response.body(), oauthError);
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (lock) {
            if (loaded) {
                return;
            }
            SageToken token = store.load();
            if (token == null && (config.getAccessToken() != null || config.getRefreshToken() != null)) {
                token = new SageToken(config.getAccessToken(), config.getRefreshToken(), "Bearer", null, null);
                store.save(token);
            }
            current = token;
            loaded = true;
        }
    }

    private void setCurrent(SageToken token) {
        current = token;
        loaded = true;
        store.save(token);
    }

    private String resolveRedirectUri(String redirectUri) {
        String resolved = redirectUri != null ? redirectUri : config.getRedirectUri();
        if (resolved == null) {
            throw new IllegalArgumentException("No redirect URI: pass one, or configure redirectUri(...) / SAGEACTIVE4J_REDIRECT_URI");
        }
        return resolved;
    }

    private void requireClientId() {
        if (config.getClientId() == null) {
            throw new SageActive4jAuthException("OAuth needs the app's client ID: configure clientId(...) or SAGEACTIVE4J_CLIENT_ID");
        }
    }

    static String formEncode(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=')
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8").replace("+", "%20"));
            }
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is required by every Java platform", e);
        }
        return sb.toString();
    }
}
