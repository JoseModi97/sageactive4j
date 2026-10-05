package io.github.josemodi97.sageactive4j.exception;

/**
 * No HTTP response was received at all: connection refused, DNS failure,
 * TLS failure, timeout, or the calling thread was interrupted.
 */
public class SageActive4jTransportException extends SageActive4jException {

    private static final long serialVersionUID = 1L;

    public SageActive4jTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
