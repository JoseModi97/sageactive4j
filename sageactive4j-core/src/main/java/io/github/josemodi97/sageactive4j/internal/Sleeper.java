package io.github.josemodi97.sageactive4j.internal;

/** Pauses between rate-limit retries; swapped for a recording fake in tests. Not part of the public API. */
public interface Sleeper {

    Sleeper SYSTEM = new Sleeper() {
        @Override
        public void sleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }
    };

    void sleep(long millis) throws InterruptedException;
}
