package io.github.josemodi97.sageactive4j.exception;

/**
 * Base class of every exception sageactive4j throws, so callers can catch
 * the whole family in one place. Unchecked, like the rest of the hierarchy.
 */
public class SageActive4jException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SageActive4jException(String message) {
        super(message);
    }

    public SageActive4jException(String message, Throwable cause) {
        super(message, cause);
    }
}
