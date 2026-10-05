package io.github.josemodi97.sageactive4j.spring.boot3;

import io.github.josemodi97.sageactive4j.Region;
import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import io.github.josemodi97.sageactive4j.auth.TokenStore;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code sageactive4j.*} settings. The starter only activates once
 * {@code sageactive4j.subscription-key} is set.
 *
 * <pre>
 * sageactive4j.region=FR
 * sageactive4j.subscription-key=${SAGE_SUBSCRIPTION_KEY}
 * sageactive4j.organization-id=...
 * sageactive4j.client-id=...
 * sageactive4j.client-secret=${SAGE_CLIENT_SECRET}
 * sageactive4j.redirect-uri=https://app.example.com/sage/oauth/callback
 * sageactive4j.token-store.file=/var/lib/myapp/sage-tokens.properties
 * </pre>
 */
@ConfigurationProperties(prefix = "sageactive4j")
public class SageActive4jProperties {

    /** Legislation whose gateway to call: FR, ES, DE or PT. */
    private Region region = Region.FR;
    /** Overrides the region's gateway (a proxy, or a stub server in tests). */
    private String baseUrl;
    /** The app's subscription key from the Sage Developer Center (required; activates the starter). */
    private String subscriptionKey;
    /** Header carrying the subscription key. */
    private String subscriptionKeyHeader = SageActive4jConfig.DEFAULT_SUBSCRIPTION_KEY_HEADER;
    /** Default organization sent as X-OrganizationId. */
    private String organizationId;
    /** Optional X-Country-Code legislation context (fr, es, de, pt). */
    private String countryCode;
    /** OAuth client id. */
    private String clientId;
    /** OAuth client secret; leave empty for public clients using PKCE. */
    private String clientSecret;
    /** An access token obtained elsewhere (seed). */
    private String accessToken;
    /** A refresh token obtained elsewhere (seed); tokens are then renewed automatically. */
    private String refreshToken;
    /** Space-separated OAuth scopes. */
    private String scopes = SageActive4jConfig.DEFAULT_SCOPES;
    /** OAuth redirect URI registered for the app. */
    private String redirectUri;
    /** SBC Auth authorization endpoint. */
    private String authUrl = SageActive4jConfig.DEFAULT_AUTH_URL;
    /** SBC Auth token endpoint. */
    private String tokenUrl = SageActive4jConfig.DEFAULT_TOKEN_URL;
    /** SBC Auth revocation endpoint. */
    private String revocationUrl = SageActive4jConfig.DEFAULT_REVOCATION_URL;
    /** Connection timeout. */
    private Duration connectTimeout = Duration.ofMillis(SageActive4jConfig.DEFAULT_CONNECT_TIMEOUT_MILLIS);
    /** Response timeout. */
    private Duration readTimeout = Duration.ofMillis(SageActive4jConfig.DEFAULT_READ_TIMEOUT_MILLIS);
    /** Retries after HTTP 429 (0 disables retrying). */
    private int maxRetryAttempts = SageActive4jConfig.DEFAULT_MAX_RETRY_ATTEMPTS;
    /** Mutations allowed in flight at once (Sage Active allows 10 per app; 0 = unlimited). */
    private int maxConcurrentMutations = SageActive4jConfig.DEFAULT_MAX_CONCURRENT_MUTATIONS;

    private final TokenStoreProperties tokenStore = new TokenStoreProperties();
    private final OAuth oauth = new OAuth();
    private final Health health = new Health();

    /** Builds the core configuration, with the given token store. */
    public SageActive4jConfig toConfig(TokenStore store) {
        return SageActive4jConfig.builder()
                .region(region)
                .baseUrl(baseUrl)
                .subscriptionKey(subscriptionKey)
                .subscriptionKeyHeader(subscriptionKeyHeader)
                .organizationId(organizationId)
                .countryCode(countryCode)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .scopes(scopes)
                .redirectUri(redirectUri)
                .authUrl(authUrl)
                .tokenUrl(tokenUrl)
                .revocationUrl(revocationUrl)
                .connectTimeoutMillis(toMillis(connectTimeout))
                .readTimeoutMillis(toMillis(readTimeout))
                .maxRetryAttempts(maxRetryAttempts)
                .maxConcurrentMutations(maxConcurrentMutations)
                .tokenStore(store)
                .build();
    }

    private static int toMillis(Duration duration) {
        return duration == null ? 0 : (int) Math.min(Integer.MAX_VALUE, duration.toMillis());
    }

    /** Where tokens are kept. */
    public static class TokenStoreProperties {
        /**
         * File to keep tokens in (owner-only permissions, atomic writes). Empty =
         * in memory, losing rotated refresh tokens on restart. For several app
         * instances, define your own TokenStore bean instead.
         */
        private String file;

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }
    }

    /** Browser sign-in endpoints. */
    public static class OAuth {
        /**
         * Expose the sign-in endpoints. Off by default: whoever completes sign-in
         * sets the credentials the whole app uses, so protect these paths.
         */
        private boolean enabled;
        /** Path that redirects the browser to SBC Auth. */
        private String loginPath = "/sage/oauth/login";
        /** Path SBC Auth redirects back to (must match the registered redirect URI). */
        private String callbackPath = "/sage/oauth/callback";
        /** Redirect URI sent to SBC Auth; defaults to sageactive4j.redirect-uri. */
        private String redirectUri;
        /** Where to send the browser after a successful sign-in; empty = a plain confirmation page. */
        private String successUrl;
        /** Send a PKCE challenge. */
        private boolean pkce = true;
        /** How long a started sign-in stays valid. */
        private Duration maxAge = Duration.ofMinutes(10);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLoginPath() {
            return loginPath;
        }

        public void setLoginPath(String loginPath) {
            this.loginPath = loginPath;
        }

        public String getCallbackPath() {
            return callbackPath;
        }

        public void setCallbackPath(String callbackPath) {
            this.callbackPath = callbackPath;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getSuccessUrl() {
            return successUrl;
        }

        public void setSuccessUrl(String successUrl) {
            this.successUrl = successUrl;
        }

        public boolean isPkce() {
            return pkce;
        }

        public void setPkce(boolean pkce) {
            this.pkce = pkce;
        }

        public Duration getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Duration maxAge) {
            this.maxAge = maxAge;
        }
    }

    /** Actuator health contribution. */
    public static class Health {
        /** Contribute a "sageActive4j" health component (needs spring-boot-actuator). */
        private boolean enabled = true;
        /** Reuse a health result for this long, so probes don't hit Sage Active's rate limit. */
        private Duration cacheTtl = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(Duration cacheTtl) {
            this.cacheTtl = cacheTtl;
        }
    }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getSubscriptionKey() { return subscriptionKey; }
    public void setSubscriptionKey(String subscriptionKey) { this.subscriptionKey = subscriptionKey; }
    public String getSubscriptionKeyHeader() { return subscriptionKeyHeader; }
    public void setSubscriptionKeyHeader(String subscriptionKeyHeader) { this.subscriptionKeyHeader = subscriptionKeyHeader; }
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public String getAuthUrl() { return authUrl; }
    public void setAuthUrl(String authUrl) { this.authUrl = authUrl; }
    public String getTokenUrl() { return tokenUrl; }
    public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }
    public String getRevocationUrl() { return revocationUrl; }
    public void setRevocationUrl(String revocationUrl) { this.revocationUrl = revocationUrl; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public int getMaxRetryAttempts() { return maxRetryAttempts; }
    public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }
    public int getMaxConcurrentMutations() { return maxConcurrentMutations; }
    public void setMaxConcurrentMutations(int maxConcurrentMutations) { this.maxConcurrentMutations = maxConcurrentMutations; }
    public TokenStoreProperties getTokenStore() { return tokenStore; }
    public OAuth getOauth() { return oauth; }
    public Health getHealth() { return health; }

    /** Secrets masked. */
    @Override
    public String toString() {
        return "SageActive4jProperties{" + toConfig(null) + '}';
    }
}
