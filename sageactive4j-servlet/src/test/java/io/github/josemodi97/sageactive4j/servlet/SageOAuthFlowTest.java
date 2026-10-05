package io.github.josemodi97.sageactive4j.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import io.github.josemodi97.sageactive4j.SageActive4jConfig;
import io.github.josemodi97.sageactive4j.auth.InMemoryTokenStore;
import io.github.josemodi97.sageactive4j.auth.SageAuthClient;
import io.github.josemodi97.sageactive4j.auth.SageToken;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SageOAuthFlowTest {

    private static final String REDIRECT = "https://app.example.com/sage/callback";

    private HttpServer tokenServer;
    private final List<String> tokenRequests = new CopyOnWriteArrayList<>();
    private volatile int tokenStatus = 200;
    private InMemoryTokenStore store;
    private SageAuthClient auth;
    private final Map<String, Object> sessionAttributes = new HashMap<>();
    private HttpSession session;

    @BeforeEach
    void setUp() throws IOException {
        tokenServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        tokenServer.createContext("/connect/token", exchange -> {
            tokenRequests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = (tokenStatus == 200
                    ? "{\"access_token\":\"access-1\",\"refresh_token\":\"refresh-1\",\"expires_in\":28800}"
                    : "{\"error\":\"invalid_grant\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(tokenStatus, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        tokenServer.start();
        store = new InMemoryTokenStore();
        auth = new SageAuthClient(SageActive4jConfig.builder()
                .subscriptionKey("k").clientId("client-id")
                .authUrl("https://sbcauth.example/connect/authorize")
                .tokenUrl("http://127.0.0.1:" + tokenServer.getAddress().getPort() + "/connect/token")
                .tokenStore(store)
                .build());

        session = mock(HttpSession.class);
        when(session.getAttribute(anyString())).thenAnswer(i -> sessionAttributes.get(i.getArgument(0, String.class)));
        org.mockito.Mockito.doAnswer(i -> sessionAttributes.put(i.getArgument(0), i.getArgument(1)))
                .when(session).setAttribute(anyString(), org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.doAnswer(i -> sessionAttributes.remove(i.getArgument(0, String.class)))
                .when(session).removeAttribute(anyString());
    }

    @AfterEach
    void tearDown() {
        tokenServer.stop(0);
    }

    private HttpServletRequest request(Map<String, String> params) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getParameter(anyString())).thenAnswer(i -> params.get(i.getArgument(0, String.class)));
        return request;
    }

    private static Map<String, String> query(String url) {
        Map<String, String> params = new HashMap<>();
        for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
            String[] kv = pair.split("=", 2);
            params.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return params;
    }

    private static Map<String, String> callback(String state, String code) {
        Map<String, String> params = new HashMap<>();
        params.put("state", state);
        params.put("code", code);
        return params;
    }

    @Test
    void fullSignInWithPkce() {
        SageOAuthFlow flow = new SageOAuthFlow(auth, REDIRECT);

        String url = flow.begin(request(Map.of()));
        Map<String, String> sent = query(url);
        assertTrue(url.startsWith("https://sbcauth.example/connect/authorize?"));
        assertEquals(REDIRECT, sent.get("redirect_uri"));
        assertEquals("S256", sent.get("code_challenge_method"));
        assertTrue(sent.get("state").length() >= 43);

        SageToken token = flow.complete(request(callback(sent.get("state"), "the-code")));

        assertEquals("access-1", token.getAccessToken());
        assertEquals("refresh-1", store.load().getRefreshToken());
        String form = URLDecoder.decode(tokenRequests.get(0), StandardCharsets.UTF_8);
        assertTrue(form.contains("code=the-code"), form);
        assertTrue(form.contains("redirect_uri=" + REDIRECT), form);
        assertTrue(form.matches(".*code_verifier=[A-Za-z0-9_-]{43}.*"), form);
        assertFalse(sessionAttributes.containsKey(SageOAuthFlow.SESSION_ATTRIBUTE), "state must be single-use");
    }

    @Test
    void wrongStateIsRejectedAndNothingIsExchanged() {
        SageOAuthFlow flow = new SageOAuthFlow(auth, REDIRECT);
        flow.begin(request(Map.of()));

        SageOAuthCallbackException e = assertThrows(SageOAuthCallbackException.class,
                () -> flow.complete(request(callback("forged-state", "code"))));

        assertEquals(SageOAuthCallbackException.Reason.INVALID_STATE, e.getReason());
        assertTrue(tokenRequests.isEmpty());
    }

    @Test
    void aStateCannotBeReplayed() {
        SageOAuthFlow flow = new SageOAuthFlow(auth, REDIRECT);
        String state = query(flow.begin(request(Map.of()))).get("state");
        tokenStatus = 400;
        assertThrows(SageOAuthCallbackException.class, () -> flow.complete(request(callback(state, "code"))));

        tokenStatus = 200;
        SageOAuthCallbackException replay = assertThrows(SageOAuthCallbackException.class,
                () -> flow.complete(request(callback(state, "code"))));
        assertEquals(SageOAuthCallbackException.Reason.INVALID_STATE, replay.getReason());
        assertEquals(1, tokenRequests.size());
    }

    @Test
    void noSessionMeansNoValidState() {
        SageOAuthFlow flow = new SageOAuthFlow(auth, REDIRECT);
        HttpServletRequest request = request(callback("anything", "code"));
        when(request.getSession(false)).thenReturn(null);
        assertEquals(SageOAuthCallbackException.Reason.INVALID_STATE,
                assertThrows(SageOAuthCallbackException.class, () -> flow.complete(request)).getReason());
    }

    @Test
    void expiredSignInIsRejected() {
        MutableClock clock = new MutableClock();
        SageOAuthFlow flow = new SageOAuthFlow(auth, REDIRECT, true, Duration.ofMinutes(10), clock);
        String state = query(flow.begin(request(Map.of()))).get("state");
        clock.now = clock.now.plus(Duration.ofMinutes(11));

        SageOAuthCallbackException e = assertThrows(SageOAuthCallbackException.class,
                () -> flow.complete(request(callback(state, "code"))));
        assertEquals(SageOAuthCallbackException.Reason.INVALID_STATE, e.getReason());
        assertTrue(tokenRequests.isEmpty());
    }

    @Test
    void userDeclinedIsReportedWithTheOAuthError() {
        SageOAuthFlow flow = new SageOAuthFlow(auth, REDIRECT);
        flow.begin(request(Map.of()));
        Map<String, String> params = new HashMap<>();
        params.put("error", "access_denied");
        params.put("error_description", "User cancelled");

        SageOAuthCallbackException e = assertThrows(SageOAuthCallbackException.class, () -> flow.complete(request(params)));

        assertEquals(SageOAuthCallbackException.Reason.AUTHORIZATION_DENIED, e.getReason());
        assertEquals("access_denied", e.getOAuthError());
        assertFalse(sessionAttributes.containsKey(SageOAuthFlow.SESSION_ATTRIBUTE));
    }

    @Test
    void missingCode() {
        SageOAuthFlow flow = new SageOAuthFlow(auth, REDIRECT);
        String state = query(flow.begin(request(Map.of()))).get("state");
        assertEquals(SageOAuthCallbackException.Reason.MISSING_CODE, assertThrows(SageOAuthCallbackException.class,
                () -> flow.complete(request(callback(state, null)))).getReason());
    }

    @Test
    void exchangeFailureMapsTo502() {
        tokenStatus = 400;
        SageOAuthFlow flow = new SageOAuthFlow(auth, REDIRECT);
        String state = query(flow.begin(request(Map.of()))).get("state");
        SageOAuthCallbackException e = assertThrows(SageOAuthCallbackException.class,
                () -> flow.complete(request(callback(state, "code"))));
        assertEquals(SageOAuthCallbackException.Reason.EXCHANGE_FAILED, e.getReason());
        assertEquals(502, e.getReason().getHttpStatus());
    }

    @Test
    void withoutPkceNoChallengeOrVerifierIsSent() {
        SageOAuthFlow flow = new SageOAuthFlow(auth, REDIRECT, false, null, null);
        String url = flow.begin(request(Map.of()));
        assertNull(query(url).get("code_challenge"));
        flow.complete(request(callback(query(url).get("state"), "c")));
        assertFalse(tokenRequests.get(0).contains("code_verifier"));
    }

    @Test
    void handlerWritesASafeDefaultResponse() throws IOException {
        SageOAuthFlow flow = new SageOAuthFlow(auth, REDIRECT);
        flow.begin(request(Map.of()));
        CapturingResponse captured = new CapturingResponse();

        new SageOAuthCallbackHandler(flow, null, null).handle(request(callback("bad", "c")), captured.response);

        verify(captured.response).setStatus(400);
        assertEquals("Sage Active sign-in failed (INVALID_STATE).", captured.body());
        verify(captured.response).setHeader("Cache-Control", "no-store");
    }

    @Test
    void handlerNeverOverwritesACommittedResponse() throws IOException {
        SageOAuthFlow flow = new SageOAuthFlow(auth, REDIRECT);
        String state = query(flow.begin(request(Map.of()))).get("state");
        CapturingResponse captured = new CapturingResponse();
        SageToken[] seen = new SageToken[1];

        new SageOAuthCallbackHandler(flow, (req, resp, token) -> {
            seen[0] = token;
            resp.sendRedirect("/dashboard");
            when(resp.isCommitted()).thenReturn(true);
        }, null).handle(request(callback(state, "c")), captured.response);

        assertEquals("access-1", seen[0].getAccessToken());
        verify(captured.response).sendRedirect("/dashboard");
        verify(captured.response, never()).setStatus(anyInt());
        verify(captured.response, never()).setContentType(eq("text/plain;charset=UTF-8"));
    }

    private static final class CapturingResponse {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        CapturingResponse() throws IOException {
            when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener listener) {
                }

                @Override
                public void write(int b) {
                    bytes.write(b);
                }
            });
        }

        String body() {
            return bytes.toString(StandardCharsets.UTF_8);
        }
    }

    private static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-05T10:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
