/**
 * Java 9+ module descriptor, packaged into {@code META-INF/versions/9/} of a
 * multi-release jar (compiled at {@code --release 9}); the base jar entries
 * stay Java-8-only, and Java 8 simply ignores the versioned folder.
 *
 * <p>Only public API packages are exported; {@code .internal} stays
 * encapsulated even from module-path consumers. Each new public package
 * ({@code .model}, {@code .service}, {@code .exception}, {@code .auth}) is
 * added here - and in the Java 11 descriptor - as it lands, since an
 * {@code exports} clause for a package that doesn't exist yet is a compile
 * error.
 *
 * <p>Deliberately no {@code requires java.net.http}: that module doesn't
 * exist at the Java 9 platform level. {@code src/main/java11/module-info.java}
 * is the descriptor Java 11+ runtimes load instead.
 */
module io.github.josemodi97.sageactive4j.core {
    requires java.logging;

    exports io.github.josemodi97.sageactive4j;
    exports io.github.josemodi97.sageactive4j.auth;
    exports io.github.josemodi97.sageactive4j.exception;
    exports io.github.josemodi97.sageactive4j.graphql;
    exports io.github.josemodi97.sageactive4j.input;
    exports io.github.josemodi97.sageactive4j.model;
    exports io.github.josemodi97.sageactive4j.service;
}
