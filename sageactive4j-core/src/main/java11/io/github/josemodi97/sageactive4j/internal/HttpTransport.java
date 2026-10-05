package io.github.josemodi97.sageactive4j.internal;

import io.github.josemodi97.sageactive4j.exception.SageActive4jTransportException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Java 11+ HTTP transport, built on {@link HttpClient} (HTTP/2 negotiated via
 * ALPN, connection pooling). Packaged into {@code META-INF/versions/11/}, it
 * replaces the {@code HttpURLConnection} baseline on Java 11+ runtimes.
 * Public signatures must stay identical to the baseline. Not part of the
 * public API.
 */
public final class HttpTransport {

    // HttpClient's connect timeout is fixed per client, so one shared,
    // pooled client per distinct timeout value (in practice: one).
    private static final ConcurrentMap<Integer, HttpClient> CLIENTS = new ConcurrentHashMap<>();

    private HttpTransport() {
    }

    /** Which implementation is active: {@code "HttpClient"} here. Used by tests and diagnostics. */
    public static String implementation() {
        return "HttpClient";
    }

    /**
     * Sends {@code request} and returns whatever status came back - 4xx/5xx
     * are returned, not thrown, so callers can inspect the body.
     *
     * @throws SageActive4jTransportException if no response was received
     */
    public static HttpResponse send(HttpRequest request, int connectTimeoutMillis, int readTimeoutMillis) {
        HttpClient client = CLIENTS.computeIfAbsent(connectTimeoutMillis, timeout -> HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeout))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());

        java.net.http.HttpRequest.Builder builder;
        try {
            builder = java.net.http.HttpRequest.newBuilder(URI.create(request.url()))
                    .timeout(Duration.ofMillis(readTimeoutMillis));
        } catch (IllegalArgumentException e) {
            throw new SageActive4jTransportException("Invalid URL " + request.url() + ": " + e.getMessage(), e);
        }
        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        byte[] body = request.body();
        builder.method(request.method(), body == null
                ? java.net.http.HttpRequest.BodyPublishers.noBody()
                : java.net.http.HttpRequest.BodyPublishers.ofByteArray(body));

        try {
            java.net.http.HttpResponse<byte[]> response =
                    client.send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            return new HttpResponse(request.url(), response.statusCode(), response.headers().map(),
                    new String(response.body(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SageActive4jTransportException(
                    request.method() + " " + request.url() + " failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SageActive4jTransportException(
                    request.method() + " " + request.url() + " was interrupted", e);
        }
    }
}
