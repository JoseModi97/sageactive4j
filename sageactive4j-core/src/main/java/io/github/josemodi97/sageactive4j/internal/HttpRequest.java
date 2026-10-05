package io.github.josemodi97.sageactive4j.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An outgoing HTTP request: method, URL, headers, and an optional body.
 * Immutable once built. Not part of the public API.
 *
 * <p>{@code Content-Length}, {@code Host} and {@code Connection} are managed
 * by the transport and must not be set here (the Java 11+ HttpClient
 * rejects them outright).
 */
public final class HttpRequest {

    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final byte[] body;

    public HttpRequest(String method, String url, Map<String, String> headers, byte[] body) {
        this.method = method;
        this.url = url;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<String, String>(headers));
        this.body = body;
    }

    public String method() {
        return method;
    }

    public String url() {
        return url;
    }

    public Map<String, String> headers() {
        return headers;
    }

    /** The body, or {@code null} for none. */
    public byte[] body() {
        return body;
    }
}
