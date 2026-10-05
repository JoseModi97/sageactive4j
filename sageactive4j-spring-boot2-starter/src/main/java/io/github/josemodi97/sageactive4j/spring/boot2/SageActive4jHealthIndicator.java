package io.github.josemodi97.sageactive4j.spring.boot2;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.exception.SageActive4jApiException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jAuthException;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Reports whether Sage Active answers with the configured credentials, by
 * reading the signed-in user's profile. Results are cached (default 30 s)
 * so frequent probes don't eat into the 3,000 requests/minute budget.
 *
 * <p>Details never include personal data or response bodies: only the
 * region and organization on success, and the failure's type and HTTP
 * status otherwise.
 */
public class SageActive4jHealthIndicator implements HealthIndicator {

    private final SageActive4jClient client;
    private final Duration cacheTtl;
    private final Clock clock;

    private volatile Health cached;
    private volatile long cachedAtMillis;

    public SageActive4jHealthIndicator(SageActive4jClient client, Duration cacheTtl) {
        this(client, cacheTtl, Clock.systemUTC());
    }

    SageActive4jHealthIndicator(SageActive4jClient client, Duration cacheTtl, Clock clock) {
        this.client = client;
        this.cacheTtl = cacheTtl == null ? Duration.ZERO : cacheTtl;
        this.clock = clock;
    }

    @Override
    public Health health() {
        Health current = cached;
        if (current != null && clock.millis() - cachedAtMillis < cacheTtl.toMillis()) {
            return current;
        }
        Health fresh = check();
        cached = fresh;
        cachedAtMillis = clock.millis();
        return fresh;
    }

    private Health check() {
        try {
            client.users().getProfile();
            Health.Builder up = Health.up().withDetail("region", client.getConfig().getRegion().name());
            if (client.getOrganizationId() != null) {
                up.withDetail("organizationId", client.getOrganizationId());
            }
            return up.build();
        } catch (SageActive4jAuthException e) {
            return Health.down().withDetail("error", "Not signed in or token rejected - sign in again")
                    .withDetail("httpStatus", e.getHttpStatus()).build();
        } catch (SageActive4jApiException e) {
            return Health.down().withDetail("error", e.getClass().getSimpleName())
                    .withDetail("httpStatus", e.getHttpStatus()).build();
        } catch (RuntimeException e) {
            return Health.down().withDetail("error", e.getClass().getSimpleName()).build();
        }
    }
}
