package io.github.josemodi97.sageactive4j.internal;

import io.github.josemodi97.sageactive4j.SageActive4j;
import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import io.github.josemodi97.sageactive4j.auth.SageAuthClient;
import io.github.josemodi97.sageactive4j.exception.SageActive4jApiException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jAuthException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jRateLimitException;
import io.github.josemodi97.sageactive4j.exception.SageActive4jTransportException;
import io.github.josemodi97.sageactive4j.graphql.FileUpload;
import io.github.josemodi97.sageactive4j.graphql.GraphQLRequest;
import io.github.josemodi97.sageactive4j.graphql.GraphQLResponse;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/**
 * Sends GraphQL operations to Sage Active: headers, bearer token, HTTP 429
 * retry with {@code Retry-After}/exponential backoff, one token refresh and
 * replay on HTTP 401, the 10-concurrent-mutations limit, GraphQL multipart
 * uploads, and mapping of HTTP failures to exceptions.
 *
 * <p>GraphQL-level errors are <em>returned</em> in the {@link GraphQLResponse},
 * not thrown; callers that want them thrown use the client's {@code query(...)}.
 * Not part of the public API.
 */
public final class GraphQLTransport {

    static final long BASE_BACKOFF_MILLIS = 1_000L;
    static final long MAX_BACKOFF_MILLIS = 30_000L;
    static final long MAX_RETRY_AFTER_MILLIS = 60_000L;
    static final int MAX_JITTER_MILLIS = 250;

    private final SageActive4jConfig config;
    private final SageAuthClient auth;
    private final Sleeper sleeper;
    private final Clock clock;
    private final Random random = new Random();
    /** Sage Active allows at most 10 mutations in flight per app; {@code null} = unlimited. */
    private final Semaphore mutationPermits;

    public GraphQLTransport(SageActive4jConfig config, SageAuthClient auth, Sleeper sleeper, Clock clock) {
        this.config = config;
        this.auth = auth;
        this.sleeper = sleeper;
        this.clock = clock;
        int max = config.getMaxConcurrentMutations();
        this.mutationPermits = max > 0 ? new Semaphore(max, true) : null;
    }

    /**
     * @param organizationId sent as {@code X-OrganizationId}; {@code null} omits the header
     * @throws SageActive4jRateLimitException if still rate-limited after every retry
     * @throws SageActive4jAuthException      if no token is available or Sage Active keeps answering 401
     * @throws SageActive4jApiException       on any other non-2xx answer that isn't a GraphQL error response
     * @throws SageActive4jTransportException if no response was received
     */
    public GraphQLResponse execute(GraphQLRequest request, String organizationId) {
        byte[] body = JsonWriter.write(request).getBytes(StandardCharsets.UTF_8);
        return send(body, "application/json; charset=utf-8", Collections.<String, String>emptyMap(),
                organizationId, isMutation(request.getQuery()));
    }

    /**
     * Sends a GraphQL multipart request
     * (<a href="https://github.com/jaydenseric/graphql-multipart-request-spec">spec</a>):
     * an {@code operations} part (the request, with the file variable set to
     * {@code null}), a {@code map} part binding file {@code 0} to
     * {@code variablePath}, and the file itself. Adds the
     * {@code GraphQL-preflight: 1} header Sage Active requires on uploads.
     *
     * @param variablePath where the file goes, e.g. {@code variables.input.file}
     */
    public GraphQLResponse executeMultipart(GraphQLRequest request, String variablePath, FileUpload file,
                                            String organizationId) {
        String boundary = "sageactive4j-" + UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> map = Collections.<String, Object>singletonMap("0",
                Collections.singletonList(variablePath));

        ByteArrayOutputStream out = new ByteArrayOutputStream(file.getSize() + 2048);
        writePart(out, boundary, "operations", null, "application/json",
                JsonWriter.write(request).getBytes(StandardCharsets.UTF_8));
        writePart(out, boundary, "map", null, "application/json",
                JsonWriter.write(map).getBytes(StandardCharsets.UTF_8));
        writePart(out, boundary, "0", file.getFileName(), file.getContentType(), file.getContent());
        writeAscii(out, "--" + boundary + "--\r\n");

        Map<String, String> extra = Collections.singletonMap("GraphQL-preflight", "1");
        return send(out.toByteArray(), "multipart/form-data; boundary=" + boundary, extra, organizationId,
                isMutation(request.getQuery()));
    }

    private GraphQLResponse send(byte[] body, String contentType, Map<String, String> extraHeaders,
                                 String organizationId, boolean mutation) {
        if (mutation && mutationPermits != null) {
            try {
                mutationPermits.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SageActive4jTransportException("Interrupted while waiting for a mutation slot", e);
            }
        }
        try {
            return sendWithRetries(body, contentType, extraHeaders, organizationId);
        } finally {
            if (mutation && mutationPermits != null) {
                mutationPermits.release();
            }
        }
    }

    private GraphQLResponse sendWithRetries(byte[] body, String contentType, Map<String, String> extraHeaders,
                                            String organizationId) {
        String url = config.getGraphQLUrl();
        int rateLimitRetries = 0;
        boolean refreshedAfter401 = false;

        while (true) {
            String token = auth.getValidAccessToken();
            Map<String, String> headers = headers(token, organizationId, contentType);
            headers.putAll(extraHeaders);
            HttpResponse response = HttpTransport.send(new HttpRequest("POST", url, headers, body),
                    config.getConnectTimeoutMillis(), config.getReadTimeoutMillis());

            if (response.status() == 429) {
                if (rateLimitRetries >= config.getMaxRetryAttempts()) {
                    throw new SageActive4jRateLimitException("Sage Active rate limit exceeded (3,000 requests per app "
                            + "per minute); gave up after " + (rateLimitRetries + 1) + " attempt(s)",
                            response.body(), rateLimitRetries + 1);
                }
                rateLimitRetries++;
                pause(retryDelayMillis(response, rateLimitRetries));
                continue;
            }

            if (response.status() == 401) {
                if (!refreshedAfter401) {
                    refreshedAfter401 = true;
                    auth.forceRefresh(token);
                    continue;
                }
                throw new SageActive4jAuthException("Sage Active rejected the access token (HTTP 401) even after "
                        + "refreshing it. Check that the app is allowed to access this organization and legislation.",
                        401, response.body(), null);
            }

            return toGraphQLResponse(response);
        }
    }

    private GraphQLResponse toGraphQLResponse(HttpResponse response) {
        GraphQLResponse parsed = null;
        try {
            parsed = GraphQLResponse.fromJson(response.status(), JsonReader.parseObject(response.body()), response.body());
        } catch (RuntimeException ignored) {
            // not JSON (e.g. a gateway HTML error page) - handled below
        }

        if (response.isSuccess()) {
            if (parsed == null) {
                throw new SageActive4jApiException("Sage Active returned HTTP " + response.status()
                        + " but the body is not a GraphQL response: " + abbreviate(response.body()),
                        response.status(), response.body());
            }
            return parsed;
        }
        // GraphQL servers may answer validation errors with a 4xx and a
        // regular errors[] body; that's still a GraphQL response.
        if (parsed != null && parsed.hasErrors()) {
            return parsed;
        }
        throw new SageActive4jApiException("Sage Active returned HTTP " + response.status() + hint(response.status())
                + ": " + abbreviate(response.body()), response.status(), response.body());
    }

    private Map<String, String> headers(String accessToken, String organizationId, String contentType) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", contentType);
        headers.put("Accept", "application/json");
        headers.put("User-Agent", SageActive4j.userAgent());
        headers.put("Authorization", "Bearer " + accessToken);
        headers.put(config.getSubscriptionKeyHeader(), config.getSubscriptionKey());
        if (organizationId != null && !organizationId.trim().isEmpty()) {
            headers.put("X-OrganizationId", organizationId.trim());
        }
        if (config.getCountryCode() != null) {
            headers.put("X-Country-Code", config.getCountryCode());
        }
        return headers;
    }

    /**
     * Whether the document's operation is a mutation. Skips leading
     * whitespace, commas and {@code #} comments; a document holding several
     * operations is classified by its first one.
     */
    static boolean isMutation(String query) {
        int i = 0;
        int n = query.length();
        while (i < n) {
            char c = query.charAt(i);
            if (c == '#') {
                while (i < n && query.charAt(i) != '\n' && query.charAt(i) != '\r') {
                    i++;
                }
            } else if (Character.isWhitespace(c) || c == ',' || c == '﻿') {
                i++;
            } else {
                break;
            }
        }
        return query.startsWith("mutation", i)
                && (i + 8 == n || !Character.isLetterOrDigit(query.charAt(i + 8)) && query.charAt(i + 8) != '_');
    }

    private static void writePart(ByteArrayOutputStream out, String boundary, String name, String fileName,
                                  String contentType, byte[] content) {
        StringBuilder head = new StringBuilder("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"").append(name).append('"');
        if (fileName != null) {
            head.append("; filename=\"").append(sanitizeFileName(fileName)).append('"');
        }
        head.append("\r\nContent-Type: ").append(contentType).append("\r\n\r\n");
        byte[] headBytes = head.toString().getBytes(StandardCharsets.UTF_8);
        out.write(headBytes, 0, headBytes.length);
        out.write(content, 0, content.length);
        writeAscii(out, "\r\n");
    }

    /** Keeps a filename from breaking out of its header: no quotes, backslashes or line breaks. */
    static String sanitizeFileName(String fileName) {
        return fileName.replace("\r", "").replace("\n", "").replace("\"", "%22").replace("\\", "_");
    }

    private static void writeAscii(ByteArrayOutputStream out, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        out.write(bytes, 0, bytes.length);
    }

    /** {@code Retry-After} (seconds or HTTP-date) if present, else exponential backoff with jitter. */
    long retryDelayMillis(HttpResponse response, int retryNumber) {
        String retryAfter = response.header("Retry-After");
        if (retryAfter != null) {
            String value = retryAfter.trim();
            try {
                long seconds = Long.parseLong(value);
                return Math.min(Math.max(seconds, 0L) * 1000L, MAX_RETRY_AFTER_MILLIS);
            } catch (NumberFormatException notSeconds) {
                try {
                    ZonedDateTime at = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
                    long millis = at.toInstant().toEpochMilli() - clock.millis();
                    return Math.min(Math.max(millis, 0L), MAX_RETRY_AFTER_MILLIS);
                } catch (DateTimeParseException ignored) {
                    // unusable header: fall back to backoff
                }
            }
        }
        long backoff = BASE_BACKOFF_MILLIS << Math.min(retryNumber - 1, 10);
        return Math.min(backoff, MAX_BACKOFF_MILLIS) + random.nextInt(MAX_JITTER_MILLIS + 1);
    }

    private void pause(long millis) {
        try {
            sleeper.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SageActive4jTransportException("Interrupted while waiting to retry a rate-limited request", e);
        }
    }

    private static String hint(int status) {
        switch (status) {
            case 403:
                return " (forbidden: check the subscription key, and that the user may access this organization)";
            case 404:
                return " (not found: check the region or baseUrl)";
            default:
                return status >= 500 ? " (Sage Active server error)" : "";
        }
    }

    static String abbreviate(String body) {
        if (body == null || body.isEmpty()) {
            return "<empty body>";
        }
        return body.length() <= 2_000 ? body : body.substring(0, 2_000) + "... (" + body.length() + " chars)";
    }
}
