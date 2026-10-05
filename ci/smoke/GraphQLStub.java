import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Minimal stand-in for Sage Active used by the CLI smoke tests: answers every
 * POST /graphql with a userProfile. Java 8 source.
 *
 * Usage: java -cp <dir> GraphQLStub <port>
 */
public class GraphQLStub {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", Integer.parseInt(args[0])), 0);
        server.createContext("/graphql", exchange -> {
            try (InputStream in = exchange.getRequestBody()) {
                byte[] buf = new byte[8192];
                while (in.read(buf) != -1) {
                    // drain
                }
            }
            byte[] body = "{\"data\":{\"userProfile\":{\"id\":\"u1\",\"fullName\":\"CI Smoke\"}}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        System.out.println("GraphQL stub listening on " + args[0]);
    }
}
