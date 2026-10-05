package io.github.josemodi97.sageactive4j.spring.boot2;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** A local stand-in for SBC Auth ({@code /connect/token}) and Sage Active ({@code /graphql}). */
final class FakeSage implements AutoCloseable {

    final HttpServer server;
    final List<String> tokenRequests = new CopyOnWriteArrayList<>();
    final List<String> graphqlAuthorizations = new CopyOnWriteArrayList<>();
    volatile int graphqlStatus = 200;

    FakeSage() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/connect/token", exchange -> {
            tokenRequests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            reply(exchange, 200, "{\"access_token\":\"from-callback\",\"refresh_token\":\"r1\",\"expires_in\":28800}");
        });
        server.createContext("/graphql", exchange -> {
            exchange.getRequestBody().readAllBytes();
            graphqlAuthorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            reply(exchange, graphqlStatus, graphqlStatus == 200
                    ? "{\"data\":{\"userProfile\":{\"id\":\"u1\",\"fullName\":\"Ada Lovelace\"}}}"
                    : "{\"message\":\"down\"}");
        });
        server.start();
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
