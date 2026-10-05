package io.github.josemodi97.sageactive4j.exception;

/**
 * Authentication failed: no usable token is available, SBC Auth rejected a
 * code exchange or refresh, or Sage Active kept answering 401 after a
 * refresh. Usually means the user has to sign in again.
 */
public class SageActive4jAuthException extends SageActive4jApiException {

    private static final long serialVersionUID = 1L;

    private final String oauthError;

    public SageActive4jAuthException(String message) {
        this(message, NO_HTTP_STATUS, null, null);
    }

    public SageActive4jAuthException(String message, int httpStatus, String responseBody, String oauthError) {
        super(message, httpStatus, responseBody);
        this.oauthError = oauthError;
    }

    /**
     * The OAuth 2.0 {@code error} code from SBC Auth (e.g. {@code invalid_grant}
     * for an expired or revoked refresh token), or {@code null}.
     */
    public String getOAuthError() {
        return oauthError;
    }
}
