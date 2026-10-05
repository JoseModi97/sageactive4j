import com.sun.net.httpserver.HttpServer;
import io.github.josemodi97.sageactive4j.SageActive4j;
import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * End-to-end smoke test of a packaged sageactive4j-core jar against a local
 * stub GraphQL server. Java 8 source, so the same file runs on every JDK CI
 * covers.
 *
 * Usage: java -cp <jar>:<dir> Smoke [expectedTransport]
 *   expectedTransport: HttpURLConnection or HttpClient (checked via the
 *   internal class, so only meaningful on the classpath), or omitted.
 */
public class Smoke {

    public static void main(String[] args) throws Exception {
        final AtomicReference<String> apiKey = new AtomicReference<String>();
        final AtomicReference<String> userAgent = new AtomicReference<String>();
        final AtomicReference<String> org = new AtomicReference<String>();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/graphql", exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("x-api-key"));
            userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            org.set(exchange.getRequestHeaders().getFirst("X-OrganizationId"));
            byte[] body = "{\"data\":{\"userProfile\":{\"fullName\":\"Smoke Test\"}}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            SageActive4jClient sage = new SageActive4jClient(SageActive4jConfig.builder()
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .subscriptionKey("smoke-key")
                    .accessToken("smoke-token")
                    .organizationId("smoke-org")
                    .build());

            Map<String, Object> data = sage.query("{ userProfile { fullName } }");
            Object profile = data.get("userProfile");
            check(profile instanceof Map && "Smoke Test".equals(((Map<?, ?>) profile).get("fullName")),
                    "unexpected data: " + data);
            check("smoke-key".equals(apiKey.get()), "x-api-key not sent: " + apiKey.get());
            check("smoke-org".equals(org.get()), "X-OrganizationId not sent: " + org.get());
            check(userAgent.get() != null && userAgent.get().startsWith("sageactive4j/")
                    && !userAgent.get().startsWith("sageactive4j/dev"), "bad User-Agent: " + userAgent.get());

            // A typed call loads its GraphQL document from the jar's resources -
            // on the module path that only works if the resources are readable
            // from inside the named module. userProfile is organization-independent,
            // so it must also drop the X-OrganizationId header.
            String typedName = sage.users().getProfile().getFullName();
            check("Smoke Test".equals(typedName), "typed userProfile returned: " + typedName);
            check(org.get() == null, "userProfile must not send X-OrganizationId, sent: " + org.get());
            sage.close();

            if (args.length > 0) {
                Object transport = Class.forName("io.github.josemodi97.sageactive4j.internal.HttpTransport")
                        .getMethod("implementation").invoke(null);
                check(args[0].equals(transport), "expected transport " + args[0] + ", got " + transport);
            }
            System.out.println("OK on Java " + System.getProperty("java.version") + " | " + SageActive4j.userAgent()
                    + (args.length > 0 ? " | transport " + args[0] : ""));
        } finally {
            server.stop(0);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
