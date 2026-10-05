package io.github.josemodi97.sageactive4j.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import io.github.josemodi97.sageactive4j.auth.SageAuthClient;
import io.github.josemodi97.sageactive4j.testsupport.MutableClock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RetryDelayTest {

    private final MutableClock clock = new MutableClock(1_790_000_000_000L);
    private final SageActive4jConfig config = SageActive4jConfig.builder().subscriptionKey("k").build();
    private final GraphQLTransport transport =
            new GraphQLTransport(config, new SageAuthClient(config, clock), Sleeper.SYSTEM, clock);

    private static HttpResponse withRetryAfter(String value) {
        Map<String, List<String>> headers = value == null
                ? Collections.<String, List<String>>emptyMap()
                : Collections.singletonMap("Retry-After", Arrays.asList(value));
        return new HttpResponse("http://x", 429, headers, "");
    }

    @Test
    void honoursRetryAfterSeconds() {
        assertEquals(7_000L, transport.retryDelayMillis(withRetryAfter("7"), 1));
        assertEquals(0L, transport.retryDelayMillis(withRetryAfter("0"), 1));
    }

    @Test
    void honoursRetryAfterHttpDate() {
        String inFiveSeconds = DateTimeFormatter.RFC_1123_DATE_TIME
                .format(Instant.ofEpochMilli(clock.millis() + 5_000L).atZone(ZoneOffset.UTC));
        assertEquals(5_000L, transport.retryDelayMillis(withRetryAfter(inFiveSeconds), 1));
    }

    @Test
    void capsAbsurdRetryAfter() {
        assertEquals(GraphQLTransport.MAX_RETRY_AFTER_MILLIS, transport.retryDelayMillis(withRetryAfter("86400"), 1));
    }

    @Test
    void backsOffExponentiallyWithJitterWhenNoHeader() {
        for (int retry = 1; retry <= 4; retry++) {
            long base = 1_000L << (retry - 1);
            long delay = transport.retryDelayMillis(withRetryAfter(null), retry);
            assertTrue(delay >= base && delay <= base + GraphQLTransport.MAX_JITTER_MILLIS,
                    "retry " + retry + " delay " + delay);
        }
        long late = transport.retryDelayMillis(withRetryAfter(null), 20);
        assertTrue(late <= GraphQLTransport.MAX_BACKOFF_MILLIS + GraphQLTransport.MAX_JITTER_MILLIS);
    }

    @Test
    void unparseableRetryAfterFallsBackToBackoff() {
        long delay = transport.retryDelayMillis(withRetryAfter("soon-ish"), 1);
        assertTrue(delay >= 1_000L && delay <= 1_000L + GraphQLTransport.MAX_JITTER_MILLIS);
    }
}
