package io.github.josemodi97.sageactive4j.testsupport;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

/** A clock tests can move forward, to exercise token expiry without waiting. */
public final class MutableClock extends Clock {

    private final AtomicLong millis;

    public MutableClock(long startEpochMillis) {
        this.millis = new AtomicLong(startEpochMillis);
    }

    public void advanceSeconds(long seconds) {
        millis.addAndGet(seconds * 1000L);
    }

    @Override
    public long millis() {
        return millis.get();
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(millis.get());
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
