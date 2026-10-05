package io.github.josemodi97.sageactive4j.cli;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A local stand-in for Sage Active and SBC Auth. GraphQL answers are picked
 * by the first registered fragment the query contains.
 */
final class FakeSage implements AutoCloseable {

    /** One recorded GraphQL call. */
    static final class Call {
        final String body;
        final String apiKey;
        final String organization;
        final String authorization;

        Call(String body, String apiKey, String organization, String authorization) {
            this.body = body;
            this.apiKey = apiKey;
            this.organization = organization;
            this.authorization = authorization;
        }
    }

    final HttpServer server;
    final List<Call> graphql = new CopyOnWriteArrayList<>();
    final List<String> tokenRequests = new CopyOnWriteArrayList<>();
    final List<String> revocations = new CopyOnWriteArrayList<>();
    private final Map<String, String> answers = new ConcurrentHashMap<>();

    FakeSage() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/graphql", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            graphql.add(new Call(body, exchange.getRequestHeaders().getFirst("x-api-key"),
                    exchange.getRequestHeaders().getFirst("X-OrganizationId"),
                    exchange.getRequestHeaders().getFirst("Authorization")));
            String answer = null;
            for (Map.Entry<String, String> entry : answers.entrySet()) {
                if (body.contains(entry.getKey())) {
                    answer = entry.getValue();
                }
            }
            reply(exchange, 200, answer != null ? answer : "{\"data\":{}}");
        });
        server.createContext("/connect/token", exchange -> {
            tokenRequests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            reply(exchange, 200, "{\"access_token\":\"signed-in-token\",\"refresh_token\":\"r1\",\"expires_in\":28800}");
        });
        server.createContext("/connect/revocation", exchange -> {
            revocations.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            reply(exchange, 200, "");
        });
        server.start();
    }

    /** Answers queries containing {@code fragment} with {@code {"data": dataJson}}. */
    FakeSage on(String fragment, String dataJson) {
        answers.put(fragment, "{\"data\":" + dataJson + "}");
        return this;
    }

    FakeSage onErrors(String fragment, String errorsJson) {
        answers.put(fragment, "{\"errors\":" + errorsJson + ",\"data\":null}");
        return this;
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
