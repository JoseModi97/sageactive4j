package io.github.josemodi97.sageactive4j;

import io.github.josemodi97.sageactive4j.auth.TokenStore;
import io.github.josemodi97.sageactive4j.exception.SageActive4jConfigurationException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Immutable connection settings for a {@link SageActive4jClient}.
 *
 * <p>Build one with {@link #builder()}, or load it from {@code SAGEACTIVE4J_*}
 * environment variables with {@link #fromEnvironment()} (for containers and
 * CI, where credentials are injected rather than hardcoded).
 *
 * <p>{@link #toString()} masks every secret, so a config is safe to log.
 */
public final class SageActive4jConfig {

    public static final String DEFAULT_AUTH_URL = "https://sbcauth.sage.fr/connect/authorize";
    public static final String DEFAULT_TOKEN_URL = "https://sbcauth.sage.fr/connect/token";
    public static final String DEFAULT_REVOCATION_URL = "https://sbcauth.sage.fr/connect/revocation";
    /** {@code RDSA} = read, {@code WDSA} = write Sage Active data; {@code offline_access} = refresh tokens. */
    public static final String DEFAULT_SCOPES = "RDSA WDSA offline_access";
    public static final String DEFAULT_SUBSCRIPTION_KEY_HEADER = "x-api-key";
    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 30_000;
    public static final int DEFAULT_READ_TIMEOUT_MILLIS = 60_000;
    public static final int DEFAULT_MAX_RETRY_ATTEMPTS = 3;
    public static final int DEFAULT_TOKEN_EXPIRY_SAFETY_MARGIN_SECONDS = 60;
    /** Sage Active's documented limit on mutations in flight per app. */
    public static final int DEFAULT_MAX_CONCURRENT_MUTATIONS = 10;

    private final Region region;
    private final String baseUrl;
    private final String subscriptionKey;
    private final String subscriptionKeyHeader;
    private final String organizationId;
    private final String countryCode;
    private final String clientId;
    private final String clientSecret;
    private final String accessToken;
    private final String refreshToken;
    private final String scopes;
    private final String redirectUri;
    private final String authUrl;
    private final String tokenUrl;
    private final String revocationUrl;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final int maxRetryAttempts;
    private final int tokenExpirySafetyMarginSeconds;
    private final TokenStore tokenStore;
    private final int maxConcurrentMutations;
    private final Executor executor;

    private SageActive4jConfig(Builder b) {
        this.region = b.region == null ? Region.FR : b.region;
        this.baseUrl = blankToNull(b.baseUrl);
        this.subscriptionKey = blankToNull(b.subscriptionKey);
        this.subscriptionKeyHeader = b.subscriptionKeyHeader == null || b.subscriptionKeyHeader.trim().isEmpty()
                ? DEFAULT_SUBSCRIPTION_KEY_HEADER : b.subscriptionKeyHeader.trim();
        this.organizationId = blankToNull(b.organizationId);
        this.countryCode = blankToNull(b.countryCode);
        this.clientId = blankToNull(b.clientId);
        this.clientSecret = blankToNull(b.clientSecret);
        this.accessToken = blankToNull(b.accessToken);
        this.refreshToken = blankToNull(b.refreshToken);
        this.scopes = b.scopes == null || b.scopes.trim().isEmpty() ? DEFAULT_SCOPES : b.scopes.trim();
        this.redirectUri = blankToNull(b.redirectUri);
        this.authUrl = b.authUrl == null || b.authUrl.trim().isEmpty() ? DEFAULT_AUTH_URL : b.authUrl.trim();
        this.tokenUrl = b.tokenUrl == null || b.tokenUrl.trim().isEmpty() ? DEFAULT_TOKEN_URL : b.tokenUrl.trim();
        this.revocationUrl = b.revocationUrl == null || b.revocationUrl.trim().isEmpty()
                ? DEFAULT_REVOCATION_URL : b.revocationUrl.trim();
        this.connectTimeoutMillis = b.connectTimeoutMillis > 0 ? b.connectTimeoutMillis : DEFAULT_CONNECT_TIMEOUT_MILLIS;
        this.readTimeoutMillis = b.readTimeoutMillis > 0 ? b.readTimeoutMillis : DEFAULT_READ_TIMEOUT_MILLIS;
        this.maxRetryAttempts = b.maxRetryAttempts >= 0 ? b.maxRetryAttempts : DEFAULT_MAX_RETRY_ATTEMPTS;
        this.tokenExpirySafetyMarginSeconds = b.tokenExpirySafetyMarginSeconds >= 0
                ? b.tokenExpirySafetyMarginSeconds : DEFAULT_TOKEN_EXPIRY_SAFETY_MARGIN_SECONDS;
        this.tokenStore = b.tokenStore;
        this.maxConcurrentMutations = b.maxConcurrentMutations >= 0
                ? b.maxConcurrentMutations : DEFAULT_MAX_CONCURRENT_MUTATIONS;
        this.executor = b.executor;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reads {@code SAGEACTIVE4J_REGION}, {@code _BASE_URL},
     * {@code _SUBSCRIPTION_KEY}, {@code _SUBSCRIPTION_KEY_HEADER},
     * {@code _ORGANIZATION_ID}, {@code _COUNTRY_CODE}, {@code _CLIENT_ID},
     * {@code _CLIENT_SECRET}, {@code _ACCESS_TOKEN}, {@code _REFRESH_TOKEN},
     * {@code _SCOPES}, {@code _REDIRECT_URI}, {@code _AUTH_URL},
     * {@code _TOKEN_URL}, {@code _REVOCATION_URL},
     * {@code _CONNECT_TIMEOUT_MILLIS}, {@code _READ_TIMEOUT_MILLIS} and
     * {@code _MAX_RETRY_ATTEMPTS} from the process environment.
     */
    public static SageActive4jConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    /** As {@link #fromEnvironment()}, reading from the given map instead (useful in tests). */
    public static SageActive4jConfig fromEnvironment(Map<String, String> env) {
        return builderFromEnvironment(env).build();
    }

    /** A builder pre-filled from {@code env}, for overriding individual settings afterwards. */
    public static Builder builderFromEnvironment(Map<String, String> env) {
        Map<String, String> e = env == null ? Collections.<String, String>emptyMap() : env;
        Builder b = builder()
                .baseUrl(e.get("SAGEACTIVE4J_BASE_URL"))
                .subscriptionKey(e.get("SAGEACTIVE4J_SUBSCRIPTION_KEY"))
                .subscriptionKeyHeader(e.get("SAGEACTIVE4J_SUBSCRIPTION_KEY_HEADER"))
                .organizationId(e.get("SAGEACTIVE4J_ORGANIZATION_ID"))
                .countryCode(e.get("SAGEACTIVE4J_COUNTRY_CODE"))
                .clientId(e.get("SAGEACTIVE4J_CLIENT_ID"))
                .clientSecret(e.get("SAGEACTIVE4J_CLIENT_SECRET"))
                .accessToken(e.get("SAGEACTIVE4J_ACCESS_TOKEN"))
                .refreshToken(e.get("SAGEACTIVE4J_REFRESH_TOKEN"))
                .scopes(e.get("SAGEACTIVE4J_SCOPES"))
                .redirectUri(e.get("SAGEACTIVE4J_REDIRECT_URI"))
                .authUrl(e.get("SAGEACTIVE4J_AUTH_URL"))
                .tokenUrl(e.get("SAGEACTIVE4J_TOKEN_URL"))
                .revocationUrl(e.get("SAGEACTIVE4J_REVOCATION_URL"));
        String region = blankToNull(e.get("SAGEACTIVE4J_REGION"));
        if (region != null) {
            try {
                b.region(Region.parse(region));
            } catch (IllegalArgumentException ex) {
                throw new SageActive4jConfigurationException("SAGEACTIVE4J_REGION: " + ex.getMessage());
            }
        }
        b.connectTimeoutMillis(parseInt(e, "SAGEACTIVE4J_CONNECT_TIMEOUT_MILLIS", 0));
        b.readTimeoutMillis(parseInt(e, "SAGEACTIVE4J_READ_TIMEOUT_MILLIS", 0));
        b.maxRetryAttempts(parseInt(e, "SAGEACTIVE4J_MAX_RETRY_ATTEMPTS", -1));
        return b;
    }

    /**
     * Checks that the settings can work at all, before any network call.
     *
     * @throws SageActive4jConfigurationException naming the missing setting
     *         and how to provide it
     */
    public void validate() {
        if (subscriptionKey == null) {
            throw missing("subscriptionKey", "SAGEACTIVE4J_SUBSCRIPTION_KEY",
                    "Every Sage Active call needs the subscription key from your app in the Sage Developer Center.");
        }
        if (refreshToken != null && clientId == null) {
            throw missing("clientId", "SAGEACTIVE4J_CLIENT_ID",
                    "A refresh token can only be redeemed together with the app's client ID.");
        }
    }

    private static SageActive4jConfigurationException missing(String setting, String envVar, String why) {
        return new SageActive4jConfigurationException("SageActive4jConfig is missing '" + setting + "'. " + why
                + " Set it via SageActive4jConfig.builder()." + setting + "(...) or the " + envVar
                + " environment variable.");
    }

    /** The GraphQL endpoint: {@code baseUrl} if set (a trailing {@code /graphql} is tolerated), else the region's gateway. */
    public String getGraphQLUrl() {
        String base = baseUrl != null ? baseUrl : region.getGatewayUrl();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.toLowerCase(java.util.Locale.ROOT).endsWith("/graphql")) {
            base = base.substring(0, base.length() - "/graphql".length());
        }
        return base + "/graphql";
    }

    public Region getRegion() {
        return region;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getSubscriptionKey() {
        return subscriptionKey;
    }

    public String getSubscriptionKeyHeader() {
        return subscriptionKeyHeader;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getScopes() {
        return scopes;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getRevocationUrl() {
        return revocationUrl;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    /** Extra attempts after an HTTP 429, so up to {@code maxRetryAttempts + 1} requests in total. */
    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public int getTokenExpirySafetyMarginSeconds() {
        return tokenExpirySafetyMarginSeconds;
    }

    /** The configured store, or {@code null} (the client then keeps tokens in memory). */
    public TokenStore getTokenStore() {
        return tokenStore;
    }

    /** Mutations allowed in flight at once per client (shared by its organization views); {@code 0} = unlimited. */
    public int getMaxConcurrentMutations() {
        return maxConcurrentMutations;
    }

    /** Executor for {@code SageActive4jClient.async(...)}, or {@code null} for the client's own pool. */
    public Executor getExecutor() {
        return executor;
    }

    /** A builder pre-filled with this config's values. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.region = region;
        b.baseUrl = baseUrl;
        b.subscriptionKey = subscriptionKey;
        b.subscriptionKeyHeader = subscriptionKeyHeader;
        b.organizationId = organizationId;
        b.countryCode = countryCode;
        b.clientId = clientId;
        b.clientSecret = clientSecret;
        b.accessToken = accessToken;
        b.refreshToken = refreshToken;
        b.scopes = scopes;
        b.redirectUri = redirectUri;
        b.authUrl = authUrl;
        b.tokenUrl = tokenUrl;
        b.revocationUrl = revocationUrl;
        b.connectTimeoutMillis = connectTimeoutMillis;
        b.readTimeoutMillis = readTimeoutMillis;
        b.maxRetryAttempts = maxRetryAttempts;
        b.tokenExpirySafetyMarginSeconds = tokenExpirySafetyMarginSeconds;
        b.tokenStore = tokenStore;
        b.maxConcurrentMutations = maxConcurrentMutations;
        b.executor = executor;
        return b;
    }

    @Override
    public String toString() {
        return "SageActive4jConfig{region=" + region
                + ", graphQLUrl=" + getGraphQLUrl()
                + ", subscriptionKey=" + mask(subscriptionKey)
                + ", organizationId=" + organizationId
                + ", clientId=" + clientId
                + ", clientSecret=" + mask(clientSecret)
                + ", accessToken=" + mask(accessToken)
                + ", refreshToken=" + mask(refreshToken)
                + ", scopes=" + scopes
                + '}';
    }

    static String mask(String secret) {
        return secret == null ? "null" : "****";
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static int parseInt(Map<String, String> env, String name, int fallback) {
        String value = blankToNull(env.get(name));
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new SageActive4jConfigurationException(name + " must be an integer, got '" + value + "'");
        }
    }

    /** Builder for {@link SageActive4jConfig}. Blank strings count as unset. */
    public static final class Builder {
        private Region region;
        private String baseUrl;
        private String subscriptionKey;
        private String subscriptionKeyHeader;
        private String organizationId;
        private String countryCode;
        private String clientId;
        private String clientSecret;
        private String accessToken;
        private String refreshToken;
        private String scopes;
        private String redirectUri;
        private String authUrl;
        private String tokenUrl;
        private String revocationUrl;
        private int connectTimeoutMillis;
        private int readTimeoutMillis;
        private int maxRetryAttempts = -1;
        private int tokenExpirySafetyMarginSeconds = -1;
        private TokenStore tokenStore;
        private int maxConcurrentMutations = -1;
        private Executor executor;

        private Builder() {
        }

        /**
         * Mutations allowed in flight at once. Default 10 (Sage Active's
         * documented limit); {@code 0} = unlimited; negative restores the default.
         * Extra mutations wait for a free slot instead of failing.
         */
        public Builder maxConcurrentMutations(int maxConcurrentMutations) {
            this.maxConcurrentMutations = maxConcurrentMutations;
            return this;
        }

        /**
         * Runs {@code SageActive4jClient.async(...)} calls. Default: a small
         * pool of daemon threads owned by the client and shut down by
         * {@code close()}. A supplied executor is never shut down by the client.
         */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /** Legislation whose gateway to call. Default {@link Region#FR}. */
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        /** Overrides the region's gateway entirely (a proxy, or a stub server in tests). */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /** The app's subscription key (primary or secondary). Required. */
        public Builder subscriptionKey(String subscriptionKey) {
            this.subscriptionKey = subscriptionKey;
            return this;
        }

        /** Header carrying the subscription key. Default {@code x-api-key}. */
        public Builder subscriptionKeyHeader(String subscriptionKeyHeader) {
            this.subscriptionKeyHeader = subscriptionKeyHeader;
            return this;
        }

        /**
         * Default organization (tenant) sent as {@code X-OrganizationId}.
         * Optional: {@code userProfile} and {@code organizations} work without
         * it. Override per call with {@link SageActive4jClient#withOrganization(String)}.
         */
        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        /** Optional {@code X-Country-Code} legislation context ({@code fr}, {@code es}, {@code de}, {@code pt}). */
        public Builder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        /** Omit for public clients (desktop/mobile apps), which use PKCE instead. */
        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        /** An access token obtained elsewhere. Used until it expires or is rejected. */
        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        /** A refresh token obtained elsewhere; access tokens are then fetched and renewed automatically. */
        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        /** Space-separated OAuth scopes. Default {@value SageActive4jConfig#DEFAULT_SCOPES}. */
        public Builder scopes(String scopes) {
            this.scopes = scopes;
            return this;
        }

        /** OAuth redirect URI registered for the app. */
        public Builder redirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        public Builder authUrl(String authUrl) {
            this.authUrl = authUrl;
            return this;
        }

        public Builder tokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
            return this;
        }

        public Builder revocationUrl(String revocationUrl) {
            this.revocationUrl = revocationUrl;
            return this;
        }

        /** Default 30 s. Values {@code <= 0} restore the default. */
        public Builder connectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return this;
        }

        /** Default 60 s. Values {@code <= 0} restore the default. */
        public Builder readTimeoutMillis(int readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
            return this;
        }

        /** Retries after HTTP 429. Default 3; {@code 0} disables retrying; negative restores the default. */
        public Builder maxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
            return this;
        }

        /** Refresh this many seconds before the access token expires. Default 60. */
        public Builder tokenExpirySafetyMarginSeconds(int tokenExpirySafetyMarginSeconds) {
            this.tokenExpirySafetyMarginSeconds = tokenExpirySafetyMarginSeconds;
            return this;
        }

        /**
         * Where tokens are kept, including the rotated refresh token SBC Auth
         * returns on every refresh. Default: in memory, so long-running
         * services should supply a persistent store.
         */
        public Builder tokenStore(TokenStore tokenStore) {
            this.tokenStore = tokenStore;
            return this;
        }

        public SageActive4jConfig build() {
            return new SageActive4jConfig(this);
        }

        /** Builds this configuration and returns a ready-to-use {@link SageActive4jClient}. */
        public SageActive4jClient buildClient() {
            return new SageActive4jClient(build());
        }
    }
}
