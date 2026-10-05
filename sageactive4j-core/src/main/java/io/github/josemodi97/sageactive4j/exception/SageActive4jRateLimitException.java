package io.github.josemodi97.sageactive4j.exception;

/**
 * Sage Active kept answering HTTP 429 (3,000 requests per app per minute)
 * after every configured retry.
 */
public class SageActive4jRateLimitException extends SageActive4jApiException {

    private static final long serialVersionUID = 1L;

    private final int attempts;

    public SageActive4jRateLimitException(String message, String responseBody, int attempts) {
        super(message, 429, responseBody);
        this.attempts = attempts;
    }

    /** How many requests were sent in total, including the first. */
    public int getAttempts() {
        return attempts;
    }
}
