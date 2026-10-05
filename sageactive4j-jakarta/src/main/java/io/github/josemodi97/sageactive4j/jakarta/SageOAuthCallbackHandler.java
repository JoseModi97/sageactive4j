package io.github.josemodi97.sageactive4j.jakarta;

import io.github.josemodi97.sageactive4j.auth.SageToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Ready-made handler for the OAuth callback URL: completes the
 * {@link SageOAuthFlow}, then calls your success or failure callback. If
 * the callback doesn't write a response itself, a minimal plain-text one is
 * sent (200, or the failure reason's status) - never overwriting a response
 * the callback already committed.
 *
 * <pre>{@code
 * protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
 *     handler.handle(req, resp);
 * }
 * }</pre>
 */
public final class SageOAuthCallbackHandler {

    /** Called after the tokens were exchanged and saved. */
    public interface SuccessCallback {
        void onSuccess(HttpServletRequest request, HttpServletResponse response, SageToken token) throws IOException;
    }

    /** Called when the callback could not be completed. */
    public interface FailureCallback {
        void onFailure(HttpServletRequest request, HttpServletResponse response, SageOAuthCallbackException failure)
                throws IOException;
    }

    private final SageOAuthFlow flow;
    private final SuccessCallback onSuccess;
    private final FailureCallback onFailure;

    /** @param onSuccess may be {@code null}; @param onFailure may be {@code null} */
    public SageOAuthCallbackHandler(SageOAuthFlow flow, SuccessCallback onSuccess, FailureCallback onFailure) {
        if (flow == null) {
            throw new IllegalArgumentException("flow must not be null");
        }
        this.flow = flow;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SageToken token;
        try {
            token = flow.complete(request);
        } catch (SageOAuthCallbackException failure) {
            if (onFailure != null) {
                onFailure.onFailure(request, response, failure);
            }
            if (!response.isCommitted()) {
                // The reason only; details (SBC Auth messages) belong in logs, not in the browser.
                writeText(response, failure.getReason().getHttpStatus(),
                        "Sage Active sign-in failed (" + failure.getReason() + ").");
            }
            return;
        }
        if (onSuccess != null) {
            onSuccess.onSuccess(request, response, token);
        }
        if (!response.isCommitted()) {
            writeText(response, HttpServletResponse.SC_OK, "Signed in to Sage Active. You can close this window.");
        }
    }

    private static void writeText(HttpServletResponse response, int status, String text) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.flushBuffer();
    }
}
