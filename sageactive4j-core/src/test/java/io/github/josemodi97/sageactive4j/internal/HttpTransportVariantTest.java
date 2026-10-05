package io.github.josemodi97.sageactive4j.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.CodeSource;
import org.junit.jupiter.api.Test;

/**
 * Proves which {@link HttpTransport} actually runs. Unit tests normally run
 * against the unpackaged classes, where only the Java 8 baseline exists; the
 * {@code testJar} (Gradle) / failsafe (Maven) runs repeat the whole suite
 * against the built multi-release jar, where Java 11+ must pick the
 * HttpClient variant - so both variants pass the same stub-server tests.
 */
class HttpTransportVariantTest {

    @Test
    void theExpectedVariantIsActive() {
        CodeSource source = HttpTransport.class.getProtectionDomain().getCodeSource();
        boolean fromJar = source != null && source.getLocation().getPath().endsWith(".jar");
        boolean java11Plus = javaFeatureVersion() >= 11;
        if (Boolean.getBoolean("sageactive4j.test.fromJar")) {
            // Set by the jar-based test runs: guard against them silently
            // falling back to the classes directory, which would make this
            // test pass vacuously.
            assertTrue(fromJar, "expected classes to load from the built jar, got " + source.getLocation());
        }

        String expected = fromJar && java11Plus ? "HttpClient" : "HttpURLConnection";
        assertEquals(expected, HttpTransport.implementation(),
                "running from " + (source == null ? "?" : source.getLocation()) + " on Java " + javaFeatureVersion());
    }

    static int javaFeatureVersion() {
        String spec = System.getProperty("java.specification.version");
        return spec.startsWith("1.") ? Integer.parseInt(spec.substring(2)) : Integer.parseInt(spec);
    }
}
