package io.github.josemodi97.sageactive4j.exception;

/** A required setting is missing or invalid. Thrown before any network call is made. */
public class SageActive4jConfigurationException extends SageActive4jException {

    private static final long serialVersionUID = 1L;

    public SageActive4jConfigurationException(String message) {
        super(message);
    }
}
