package io.github.josemodi97.sageactive4j.spring.boot2;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * A real Spring Boot 2 application on embedded Tomcat, using the starter the
 * way an app would: the browser sign-in round trip through the auto-configured
 * endpoints, then the actuator health component calling "Sage Active" with the
 * token that sign-in produced.
 */
@SpringBootTest(classes = SampleApplicationIntegrationTest.SampleApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SampleApplicationIntegrationTest {

    static final FakeSage SAGE;

    static {
        try {
            SAGE = new FakeSage();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void sageProperties(DynamicPropertyRegistry registry) {
        registry.add("sageactive4j.subscription-key", () -> "sub");
        registry.add("sageactive4j.base-url", SAGE::url);
        registry.add("sageactive4j.token-url", () -> SAGE.url() + "/connect/token");
        registry.add("sageactive4j.auth-url", () -> "https://sbcauth.example/connect/authorize");
        registry.add("sageactive4j.client-id", () -> "client-id");
        registry.add("sageactive4j.organization-id", () -> "org-1");
        registry.add("sageactive4j.redirect-uri", () -> "https://app.example.com/sage/oauth/callback");
        registry.add("sageactive4j.oauth.enabled", () -> "true");
        registry.add("sageactive4j.oauth.success-url", () -> "/signed-in");
        registry.add("sageactive4j.health.cache-ttl", () -> "0s");
        registry.add("management.endpoint.health.show-details", () -> "always");
    }

    @AfterAll
    static void stopSage() {
        SAGE.close();
    }

    @LocalServerPort
    int port;

    @Autowired
    TokenEvents events;

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @Test
    void signInThroughTheAutoConfiguredEndpointsThenHealthUsesTheNewToken() throws Exception {
        HttpResponse<String> healthBefore = get("/actuator/health/sageActive4j", null);
        assertThat(healthBefore.statusCode()).isEqualTo(503);
        assertThat(healthBefore.body()).contains("\"status\":\"DOWN\"").contains("sign in again");

        HttpResponse<String> login = get("/sage/oauth/login", null);
        assertThat(login.statusCode()).isEqualTo(302);
        String location = login.headers().firstValue("Location").orElseThrow();
        assertThat(location).startsWith("https://sbcauth.example/connect/authorize?");
        assertThat(location).contains("code_challenge_method=S256");
        assertThat(login.headers().firstValue("Cache-Control")).hasValue("no-store");
        String state = param(location, "state");
        String cookie = login.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];

        HttpResponse<String> forged = get("/sage/oauth/callback?state=forged&code=c", cookie);
        assertThat(forged.statusCode()).isEqualTo(400);
        assertThat(SAGE.tokenRequests).isEmpty();
        assertThat(events.failures).hasSize(1);

        // The forged attempt consumed the state (single use), so sign in again.
        login = get("/sage/oauth/login", cookie);
        state = param(login.headers().firstValue("Location").orElseThrow(), "state");

        HttpResponse<String> callback = get("/sage/oauth/callback?state=" + state + "&code=the-code", cookie);
        assertThat(callback.statusCode()).isEqualTo(302);
        assertThat(callback.headers().firstValue("Location").orElseThrow()).endsWith("/signed-in");
        assertThat(events.acquired).hasSize(1);
        assertThat(SAGE.tokenRequests.get(0)).contains("code=the-code").contains("code_verifier=");

        HttpResponse<String> healthAfter = get("/actuator/health/sageActive4j", null);
        assertThat(healthAfter.statusCode()).isEqualTo(200);
        assertThat(healthAfter.body()).contains("\"status\":\"UP\"").contains("org-1");
        assertThat(SAGE.graphqlAuthorizations).contains("Bearer from-callback");
    }

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String param(String url, String name) {
        for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv[0].equals(name)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError(name + " not in " + url);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class SampleApp {
        @org.springframework.context.annotation.Bean
        TokenEvents tokenEvents() {
            return new TokenEvents();
        }
    }

    @Component
    static class TokenEvents {
        final List<SageTokenAcquiredEvent> acquired = new CopyOnWriteArrayList<>();
        final List<SageOAuthFailedEvent> failures = new CopyOnWriteArrayList<>();

        @EventListener
        void on(SageTokenAcquiredEvent event) {
            acquired.add(event);
        }

        @EventListener
        void on(SageOAuthFailedEvent event) {
            failures.add(event);
        }
    }
}
