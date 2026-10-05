package io.github.josemodi97.sageactive4j.cli;

/** A user-facing CLI error; only its message is printed. */
final class CliException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    CliException(String message) {
        super(message);
    }
}
