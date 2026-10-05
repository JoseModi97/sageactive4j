package io.github.josemodi97.sageactive4j;

import java.util.Locale;

/**
 * Sage Active legislations and the regional API gateway that serves each.
 * Portugal is served by the Spanish gateway.
 *
 * <p>There is no separate sandbox host: development uses a sandbox
 * organization or subscription on these same gateways. To target any other
 * host (a proxy, a stub server in tests), set
 * {@link SageActive4jConfig.Builder#baseUrl(String)}.
 */
public enum Region {
    FR("https://api.fr.active.sage.com"),
    ES("https://api.es.active.sage.com"),
    DE("https://api.de.active.sage.com"),
    PT("https://api.es.active.sage.com");

    private final String gatewayUrl;

    Region(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    /** The gateway's base URL, without the {@code /graphql} path. */
    public String getGatewayUrl() {
        return gatewayUrl;
    }

    /** Lower-case ISO country code, as sent in the optional {@code X-Country-Code} header. */
    public String getCountryCode() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Case-insensitive lookup. @throws IllegalArgumentException listing the valid values */
    public static Region parse(String value) {
        if (value != null) {
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            for (Region region : values()) {
                if (region.name().equals(normalized)) {
                    return region;
                }
            }
        }
        throw new IllegalArgumentException("Unknown Sage Active region '" + value + "'; expected one of FR, ES, DE, PT");
    }
}
