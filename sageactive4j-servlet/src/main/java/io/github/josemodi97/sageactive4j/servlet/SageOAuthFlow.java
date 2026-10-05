package io.github.josemodi97.sageactive4j.servlet;

import io.github.josemodi97.sageactive4j.auth.PkceChallenge;
import io.github.josemodi97.sageactive4j.auth.SageAuthClient;
import io.github.josemodi97.sageactive4j.auth.SageToken;
import io.github.josemodi97.sageactive4j.exception.SageActive4jException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

/**
 * The browser side of SBC Auth sign-in for servlet apps: {@link #begin}
 * remembers an unguessable {@code state} (and a PKCE verifier) in the HTTP
 * session and returns the login URL; {@link #complete} checks the callback
 * against it and exchanges the code. Tokens end up in the client's
 * {@code TokenStore}.
 *
 * <p>Security properties: the state is single-use (removed on the first
 * callback, valid or not), compared in constant time, bound to the session
 * that started sign-in, and expires after {@link #DEFAULT_MAX_AGE}.
 *
 * <p><strong>Who may sign in matters:</strong> the tokens obtained become the
 * client's credentials for every later call. Put {@code begin} behind your
 * own admin authentication.
 */
public final class SageOAuthFlow {

    /** Session attribute holding {@code createdAtMillis:state:verifier}. A plain string, so clustered sessions serialize it. */
    public static final String SESSION_ATTRIBUTE = "io.github.josemodi97.sageactive4j.oauth.pending";
    public static final Duration DEFAULT_MAX_AGE = Duration.ofMinutes(10);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SageAuthClient auth;
    private final String redirectUri;
    private final boolean pkce;
    private final Duration maxAge;
    private final Clock clock;

    /** With PKCE, and the default 10-minute window. */
    public SageOAuthFlow(SageAuthClient auth, String redirectUri) {
        this(auth, redirectUri, true, DEFAULT_MAX_AGE, Clock.systemUTC());
    }

    /**
     * @param redirectUri the callback URL registered for the app; {@code null} = the client's configured one
     * @param pkce        send a PKCE challenge (required for public clients; harmless for confidential ones)
     * @param maxAge      how long a started sign-in stays valid
     */
    public SageOAuthFlow(SageAuthClient auth, String redirectUri, boolean pkce, Duration maxAge, Clock clock) {
        if (auth == null) {
            throw new IllegalArgumentException("auth must not be null");
        }
        this.auth = auth;
        this.redirectUri = redirectUri;
        this.pkce = pkce;
        this.maxAge = maxAge == null ? DEFAULT_MAX_AGE : maxAge;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /** Starts sign-in for this browser session and returns the SBC Auth URL to send it to. */
    public String begin(HttpServletRequest request) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PkceChallenge challenge = pkce ? PkceChallenge.generate() : null;
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_ATTRIBUTE, clock.millis() + ":" + state + ":"
                + (challenge == null ? "" : challenge.getVerifier()));
        return auth.buildAuthorizationUrl(state, redirectUri, challenge);
    }

    /** {@link #begin} and redirect the browser there. */
    public void redirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(begin(request));
    }

    /**
     * Completes sign-in from the callback request: checks {@code state},
     * exchanges {@code code}, saves the tokens.
     *
     * @throws SageOAuthCallbackException with the reason it failed
     */
    public SageToken complete(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String pending = null;
        if (session != null) {
            Object value = session.getAttribute(SESSION_ATTRIBUTE);
            session.removeAttribute(SESSION_ATTRIBUTE); // single use, whatever happens next
            pending = value instanceof String ? (String) value : null;
        }

        String error = request.getParameter("error");
        if (error != null) {
            String description = request.getParameter("error_description");
            throw new SageOAuthCallbackException(SageOAuthCallbackException.Reason.AUTHORIZATION_DENIED,
                    "SBC Auth returned error '" + error + "'" + (description == null ? "" : ": " + description),
                    error, null);
        }

        String[] parts = pending == null ? null : pending.split(":", 3);
        String state = request.getParameter("state");
        if (parts == null || parts.length != 3 || state == null || !constantTimeEquals(parts[1], state)) {
            throw new SageOAuthCallbackException(SageOAuthCallbackException.Reason.INVALID_STATE,
                    "OAuth state is missing, unknown or already used - start sign-in again from this browser",
                    null, null);
        }
        long createdAt;
        try {
            createdAt = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            createdAt = 0L;
        }
        if (clock.millis() - createdAt > maxAge.toMillis()) {
            throw new SageOAuthCallbackException(SageOAuthCallbackException.Reason.INVALID_STATE,
                    "Sign-in took longer than " + maxAge.toMinutes() + " minutes - start again", null, null);
        }

        String code = request.getParameter("code");
        if (code == null || code.trim().isEmpty()) {
            throw new SageOAuthCallbackException(SageOAuthCallbackException.Reason.MISSING_CODE,
                    "The callback has no authorization code", null, null);
        }
        String verifier = parts[2].isEmpty() ? null : parts[2];
        try {
            return auth.exchangeCode(code, redirectUri, verifier);
        } catch (SageActive4jException e) {
            throw new SageOAuthCallbackException(SageOAuthCallbackException.Reason.EXCHANGE_FAILED,
                    "Exchanging the authorization code failed: " + e.getMessage(), null, e);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
