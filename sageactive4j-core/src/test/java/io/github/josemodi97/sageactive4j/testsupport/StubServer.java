package io.github.josemodi97.sageactive4j.testsupport;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * A real local HTTP server (the JDK's built-in {@link HttpServer}, no mocking
 * library) standing in for Sage Active and SBC Auth. Responses are scripted
 * per path and served in order; every request is recorded.
 */
public final class StubServer implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, Queue<Reply>> scripts = new HashMap<String, Queue<Reply>>();
    private final Map<String, Reply> fallbacks = new HashMap<String, Reply>();
    private final List<Recorded> requests = Collections.synchronizedList(new ArrayList<Recorded>());
    private final java.util.concurrent.atomic.AtomicInteger inFlight = new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger maxInFlight = new java.util.concurrent.atomic.AtomicInteger();

    public StubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", exchange -> {
            try {
                String path = exchange.getRequestURI().getPath();
                byte[] body = readAll(exchange.getRequestBody());
                requests.add(new Recorded(exchange.getRequestMethod(), path,
                        exchange.getRequestURI().getRawQuery(), exchange.getRequestHeaders(),
                        new String(body, StandardCharsets.UTF_8)));
                Reply reply = next(path);
                for (Map.Entry<String, String> header : reply.headers.entrySet()) {
                    exchange.getResponseHeaders().add(header.getKey(), header.getValue());
                }
                int now = inFlight.incrementAndGet();
                maxInFlight.accumulateAndGet(now, Math::max);
                try {
                    if (reply.delayMillis > 0) {
                        Thread.sleep(reply.delayMillis);
                    }
                } finally {
                    inFlight.decrementAndGet();
                }
                byte[] out = reply.body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(reply.status, out.length == 0 ? -1 : out.length);
                if (out.length > 0) {
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(out);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Queues a reply for {@code path}; queued replies are served once each, in order. */
    public synchronized StubServer enqueue(String path, Reply reply) {
        Queue<Reply> queue = scripts.get(path);
        if (queue == null) {
            queue = new LinkedList<Reply>();
            scripts.put(path, queue);
        }
        queue.add(reply);
        return this;
    }

    /** The reply for {@code path} once its queue is empty. */
    public synchronized StubServer always(String path, Reply reply) {
        fallbacks.put(path, reply);
        return this;
    }

    private synchronized Reply next(String path) {
        Queue<Reply> queue = scripts.get(path);
        if (queue != null && !queue.isEmpty()) {
            return queue.poll();
        }
        Reply fallback = fallbacks.get(path);
        return fallback != null ? fallback : Reply.status(599).body("{\"stub\":\"no reply scripted for " + path + "\"}");
    }

    /** The most requests that were ever being handled at the same moment. */
    public int maxInFlight() {
        return maxInFlight.get();
    }

    public List<Recorded> requests() {
        synchronized (requests) {
            return new ArrayList<Recorded>(requests);
        }
    }

    public List<Recorded> requests(String path) {
        List<Recorded> matching = new ArrayList<Recorded>();
        for (Recorded r : requests()) {
            if (r.path.equals(path)) {
                matching.add(r);
            }
        }
        return matching;
    }

    @Override
    public void close() {
        server.stop(0);
        ((java.util.concurrent.ExecutorService) server.getExecutor()).shutdownNow();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /** A scripted response. */
    public static final class Reply {
        final int status;
        final Map<String, String> headers = new HashMap<String, String>();
        String body = "";
        long delayMillis;

        private Reply(int status) {
            this.status = status;
        }

        public static Reply status(int status) {
            return new Reply(status);
        }

        public static Reply json(String json) {
            return status(200).header("Content-Type", "application/json").body(json);
        }

        public Reply header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Reply body(String body) {
            this.body = body;
            return this;
        }

        public Reply delay(long millis) {
            this.delayMillis = millis;
            return this;
        }
    }

    /** A received request. */
    public static final class Recorded {
        public final String method;
        public final String path;
        public final String query;
        public final String body;
        private final Map<String, String> headers = new HashMap<String, String>();

        Recorded(String method, String path, String query, Headers headers, String body) {
            this.method = method;
            this.path = path;
            this.query = query;
            this.body = body;
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                this.headers.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue().isEmpty() ? null : e.getValue().get(0));
            }
        }

        public String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }

        /** Decodes an {@code application/x-www-form-urlencoded} body. */
        public Map<String, String> form() {
            Map<String, String> form = new HashMap<String, String>();
            if (body.isEmpty()) {
                return form;
            }
            for (String pair : body.split("&")) {
                int eq = pair.indexOf('=');
                try {
                    form.put(java.net.URLDecoder.decode(pair.substring(0, eq), "UTF-8"),
                            java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8"));
                } catch (java.io.UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
            }
            return form;
        }
    }
}
