package io.github.josemodi97.sageactive4j;

import io.github.josemodi97.sageactive4j.internal.Sleeper;
import java.time.Clock;

/** Exposes the client's test constructor to tests in other packages. */
public final class TestClients {

    private TestClients() {
    }

    public static SageActive4jClient create(SageActive4jConfig config, Clock clock, Sleeper sleeper) {
        return new SageActive4jClient(config, clock, sleeper);
    }
}
