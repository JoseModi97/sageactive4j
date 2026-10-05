package io.github.josemodi97.sageactive4j.exception;

/**
 * Sage Active (or SBC Auth) answered, but not with a usable result: a
 * non-2xx status, or a 2xx body that couldn't be understood.
 */
public class SageActive4jApiException extends SageActive4jException {

    private static final long serialVersionUID = 1L;

    /** Value of {@link #getHttpStatus()} when the failure didn't come with an HTTP response. */
    public static final int NO_HTTP_STATUS = -1;

    private final int httpStatus;
    private final String responseBody;

    public SageActive4jApiException(String message, int httpStatus, String responseBody) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public SageActive4jApiException(String message, int httpStatus, String responseBody, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    /** The HTTP status code, or {@link #NO_HTTP_STATUS}. */
    public int getHttpStatus() {
        return httpStatus;
    }

    /** The raw response body, or {@code null}. */
    public String getResponseBody() {
        return responseBody;
    }
}
