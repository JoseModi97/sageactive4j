/**
 * Overrides {@code src/main/java9/module-info.java} for Java 11+ runtimes:
 * packaged into {@code META-INF/versions/11/}, which a Java 11+ module-path
 * launch loads in preference to the {@code versions/9/} descriptor.
 *
 * <p>The one substantive difference: {@code requires java.net.http}, which
 * the Java 11+ {@code internal.HttpTransport} variant (Phase 1) is built on.
 * Without it, a real module-path launch fails with {@code IllegalAccessError}
 * even though the classpath works fine. Keep the {@code exports} list
 * identical to the Java 9 descriptor.
 */
module io.github.josemodi97.sageactive4j.core {
    requires java.logging;
    requires java.net.http;

    exports io.github.josemodi97.sageactive4j;
    exports io.github.josemodi97.sageactive4j.auth;
    exports io.github.josemodi97.sageactive4j.exception;
    exports io.github.josemodi97.sageactive4j.graphql;
    exports io.github.josemodi97.sageactive4j.input;
    exports io.github.josemodi97.sageactive4j.model;
    exports io.github.josemodi97.sageactive4j.service;
}
