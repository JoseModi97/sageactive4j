package io.github.josemodi97.sageactive4j.servlet;

import io.github.josemodi97.sageactive4j.exception.SageActive4jException;

/**
 * An OAuth sign-in callback could not be completed. {@link #getReason()}
 * says why; the message is safe to log but should not be shown verbatim to
 * end users.
 */
public class SageOAuthCallbackException extends SageActive4jException {

    private static final long serialVersionUID = 1L;

    /** Why the callback failed, with the HTTP status a handler should answer. */
    public enum Reason {
        /** SBC Auth redirected back with an {@code error} (e.g. the user declined). */
        AUTHORIZATION_DENIED(400),
        /** Missing, unknown, reused or expired {@code state}: possible CSRF, or a stale/duplicate callback. */
        INVALID_STATE(400),
        /** No {@code code} parameter. */
        MISSING_CODE(400),
        /** SBC Auth rejected the code exchange, or could not be reached. */
        EXCHANGE_FAILED(502);

        private final int httpStatus;

        Reason(int httpStatus) {
            this.httpStatus = httpStatus;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }

    private final Reason reason;
    private final String oauthError;

    public SageOAuthCallbackException(Reason reason, String message, String oauthError, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.oauthError = oauthError;
    }

    public Reason getReason() {
        return reason;
    }

    /** The OAuth {@code error} code when SBC Auth sent one (e.g. {@code access_denied}), else {@code null}. */
    public String getOAuthError() {
        return oauthError;
    }
}
