package io.github.josemodi97.sageactive4j.internal;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** A received HTTP response, body decoded as UTF-8. Not part of the public API. */
public final class HttpResponse {

    private final String requestUrl;
    private final int status;
    private final Map<String, List<String>> headers;
    private final String body;

    public HttpResponse(String requestUrl, int status, Map<String, List<String>> headers, String body) {
        this.requestUrl = requestUrl;
        this.status = status;
        // Header names are case-insensitive; HttpURLConnection also reports
        // the status line under a null key, which is dropped here.
        Map<String, List<String>> normalized = new TreeMap<String, List<String>>();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null) {
                    normalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
                }
            }
        }
        this.headers = Collections.unmodifiableMap(normalized);
        this.body = body == null ? "" : body;
    }

    public String requestUrl() {
        return requestUrl;
    }

    public int status() {
        return status;
    }

    public String body() {
        return body;
    }

    /** First value of a header (case-insensitive), or {@code null}. */
    public String header(String name) {
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }
}
