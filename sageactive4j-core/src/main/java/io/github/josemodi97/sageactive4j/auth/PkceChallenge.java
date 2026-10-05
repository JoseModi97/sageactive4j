package io.github.josemodi97.sageactive4j.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * A PKCE (RFC 7636) verifier/challenge pair, for public clients (desktop,
 * mobile, CLI) that can't keep a client secret. Send {@link #getChallenge()}
 * with the authorization request, keep {@link #getVerifier()} private, and
 * pass it to {@link SageAuthClient#exchangeCode(String, String, String)}.
 */
public final class PkceChallenge {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String verifier;
    private final String challenge;

    private PkceChallenge(String verifier, String challenge) {
        this.verifier = verifier;
        this.challenge = challenge;
    }

    /** A fresh pair: 32 random bytes as a base64url verifier, with its S256 challenge. */
    public static PkceChallenge generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return of(base64Url(bytes));
    }

    /** The pair for an existing verifier. */
    public static PkceChallenge of(String verifier) {
        if (verifier == null || verifier.length() < 43 || verifier.length() > 128) {
            throw new IllegalArgumentException("A PKCE verifier must be 43-128 characters long");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return new PkceChallenge(verifier, base64Url(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }

    public String getVerifier() {
        return verifier;
    }

    public String getChallenge() {
        return challenge;
    }

    /** Always {@code S256}. */
    public String getMethod() {
        return "S256";
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String toString() {
        return "PkceChallenge{challenge=" + challenge + ", verifier=****}";
    }
}
