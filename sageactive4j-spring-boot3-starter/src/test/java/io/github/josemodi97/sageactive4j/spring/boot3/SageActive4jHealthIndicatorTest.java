package io.github.josemodi97.sageactive4j.spring.boot3;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class SageActive4jHealthIndicatorTest {

    private FakeSage sage;
    private final MovableClock clock = new MovableClock();

    @BeforeEach
    void start() throws Exception {
        sage = new FakeSage();
    }

    @AfterEach
    void stop() {
        sage.close();
    }

    private SageActive4jClient client(String accessToken) {
        return new SageActive4jClient(SageActive4jConfig.builder().baseUrl(sage.url()).subscriptionKey("k")
                .organizationId("org-1").accessToken(accessToken).maxRetryAttempts(0).build());
    }

    @Test
    void upWithoutPersonalData() {
        Health health = new SageActive4jHealthIndicator(client("t"), Duration.ZERO, clock).health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("organizationId", "org-1").containsEntry("region", "FR");
        assertThat(health.getDetails().toString()).doesNotContain("Ada");
    }

    @Test
    void downWhenNotSignedIn() {
        Health health = new SageActive4jHealthIndicator(client(null), Duration.ZERO, clock).health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("error").toString()).contains("sign in again");
        assertThat(sage.graphqlAuthorizations).isEmpty();
    }

    @Test
    void downOnServerErrorsWithoutTheResponseBody() {
        sage.graphqlStatus = 503;
        Health health = new SageActive4jHealthIndicator(client("t"), Duration.ZERO, clock).health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("httpStatus", 503);
        assertThat(health.getDetails().toString()).doesNotContain("down");
    }

    @Test
    void resultsAreCachedForTheTtl() {
        SageActive4jHealthIndicator indicator = new SageActive4jHealthIndicator(client("t"), Duration.ofSeconds(30), clock);
        indicator.health();
        indicator.health();
        assertThat(sage.graphqlAuthorizations).hasSize(1);
        clock.now = clock.now.plusSeconds(31);
        indicator.health();
        assertThat(sage.graphqlAuthorizations).hasSize(2);
    }

    static final class MovableClock extends Clock {
        Instant now = Instant.parse("2026-10-05T10:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
