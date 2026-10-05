package io.github.josemodi97.sageactive4j.auth;

import io.github.josemodi97.sageactive4j.internal.JsonReader;
import java.util.Map;

/**
 * An SBC Auth token set. Immutable. {@link #toString()} masks both tokens.
 */
public final class SageToken {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final String scope;
    private final Long expiresAtEpochMillis;

    /**
     * @param expiresAtEpochMillis when the access token expires, or {@code null}
     *        if unknown (e.g. a token supplied from outside) - such a token is
     *        used until Sage Active rejects it with a 401
     */
    public SageToken(String accessToken, String refreshToken, String tokenType, String scope,
                     Long expiresAtEpochMillis) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.scope = scope;
        this.expiresAtEpochMillis = expiresAtEpochMillis;
    }

    /** Parses an OAuth 2.0 token endpoint response, received at {@code nowEpochMillis}. */
    static SageToken fromTokenResponse(Map<String, Object> json, long nowEpochMillis) {
        Long expiresIn = JsonReader.getLong(json, "expires_in");
        return new SageToken(
                JsonReader.getString(json, "access_token"),
                JsonReader.getString(json, "refresh_token"),
                JsonReader.getString(json, "token_type"),
                JsonReader.getString(json, "scope"),
                expiresIn == null ? null : nowEpochMillis + expiresIn * 1000L);
    }

    public String getAccessToken() {
        return accessToken;
    }

    /** May be {@code null} (no {@code offline_access} scope granted). */
    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getScope() {
        return scope;
    }

    /** {@code null} if unknown. */
    public Long getExpiresAtEpochMillis() {
        return expiresAtEpochMillis;
    }

    /**
     * Whether the access token is missing, or expires within
     * {@code marginSeconds} of {@code nowEpochMillis}. A token with unknown
     * expiry is never considered expired.
     */
    public boolean isExpiredWithin(int marginSeconds, long nowEpochMillis) {
        if (accessToken == null || accessToken.isEmpty()) {
            return true;
        }
        return expiresAtEpochMillis != null && nowEpochMillis >= expiresAtEpochMillis - marginSeconds * 1000L;
    }

    /** Copy with {@code fallback} as refresh token if this one has none (SBC Auth may not rotate it on every refresh). */
    SageToken withRefreshTokenFallback(String fallback) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            return this;
        }
        return new SageToken(accessToken, fallback, tokenType, scope, expiresAtEpochMillis);
    }

    @Override
    public String toString() {
        return "SageToken{accessToken=" + (accessToken == null ? "null" : "****")
                + ", refreshToken=" + (refreshToken == null ? "null" : "****")
                + ", tokenType=" + tokenType
                + ", scope=" + scope
                + ", expiresAtEpochMillis=" + expiresAtEpochMillis + '}';
    }
}
