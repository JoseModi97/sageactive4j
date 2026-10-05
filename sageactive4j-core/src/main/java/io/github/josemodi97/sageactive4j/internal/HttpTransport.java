package io.github.josemodi97.sageactive4j.internal;

import io.github.josemodi97.sageactive4j.exception.SageActive4jTransportException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Java 8 baseline HTTP transport, built on {@link HttpURLConnection} so the
 * core module needs no third-party HTTP library. On Java 11+ the
 * multi-release jar replaces this class with a {@code java.net.http.HttpClient}
 * variant ({@code src/main/java11}) - the public signatures of both must stay
 * identical ({@code jar --validate} enforces it). Not part of the public API.
 */
public final class HttpTransport {

    private HttpTransport() {
    }

    /** Which implementation is active: {@code "HttpURLConnection"} here. Used by tests and diagnostics. */
    public static String implementation() {
        return "HttpURLConnection";
    }

    /**
     * Sends {@code request} and returns whatever status came back - 4xx/5xx
     * are returned, not thrown, so callers can inspect the body.
     *
     * @throws SageActive4jTransportException if no response was received
     */
    public static HttpResponse send(HttpRequest request, int connectTimeoutMillis, int readTimeoutMillis) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(request.url()).openConnection();
            connection.setRequestMethod(request.method());
            connection.setConnectTimeout(connectTimeoutMillis);
            connection.setReadTimeout(readTimeoutMillis);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            for (Map.Entry<String, String> header : request.headers().entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            byte[] body = request.body();
            if (body != null) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body);
                }
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String responseBody = stream == null ? "" : readAll(stream);
            return new HttpResponse(request.url(), status, connection.getHeaderFields(), responseBody);
        } catch (IOException e) {
            throw new SageActive4jTransportException(
                    request.method() + " " + request.url() + " failed: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        try (InputStream in = stream) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
