package io.github.josemodi97.sageactive4j.internal;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The executor behind {@code SageActive4jClient.async(...)}: the configured
 * one, or a lazily created pool of daemon threads that {@link #close()}
 * shuts down. Not part of the public API.
 */
public final class AsyncSupport {

    static final int OWNED_POOL_SIZE = 8;

    private final Executor configured;
    private ExecutorService owned;
    private boolean closed;

    public AsyncSupport(Executor configured) {
        this.configured = configured;
    }

    public synchronized Executor executor() {
        if (configured != null) {
            return configured;
        }
        if (closed) {
            throw new IllegalStateException("This SageActive4jClient has been closed");
        }
        if (owned == null) {
            final AtomicInteger counter = new AtomicInteger();
            owned = Executors.newFixedThreadPool(OWNED_POOL_SIZE, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable task) {
                    Thread thread = new Thread(task, "sageactive4j-async-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            });
        }
        return owned;
    }

    public synchronized void close() {
        closed = true;
        if (owned != null) {
            owned.shutdown();
            owned = null;
        }
    }
}
