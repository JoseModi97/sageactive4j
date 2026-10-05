package io.github.josemodi97.sageactive4j.testsupport;

import io.github.josemodi97.sageactive4j.internal.Sleeper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Records requested pauses instead of sleeping, so retry tests run instantly. */
public final class RecordingSleeper implements Sleeper {

    private final List<Long> pauses = Collections.synchronizedList(new ArrayList<Long>());

    @Override
    public void sleep(long millis) {
        pauses.add(millis);
    }

    public List<Long> pauses() {
        synchronized (pauses) {
            return new ArrayList<Long>(pauses);
        }
    }
}
