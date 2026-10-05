package io.github.josemodi97.sageactive4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Library identity: name, version, and the {@code User-Agent} value every
 * request to Sage Active carries, so API-side logs can tell sageactive4j
 * traffic (and which release of it) apart from other clients.
 */
public final class SageActive4j {

    /** Library name, as used in the {@code User-Agent} header. */
    public static final String NAME = "sageactive4j";

    private static final String UNKNOWN_VERSION = "dev";

    // Read from a build-filtered resource rather than the manifest's
    // Implementation-Version: a jar loaded as a named module on the module
    // path doesn't expose manifest package attributes, so the manifest
    // approach silently reports nothing there.
    private static final String VERSION = loadVersion();

    private SageActive4j() {
    }

    /**
     * The library version this jar was built as; {@code "dev"} when the
     * version resource is missing or unfiltered (e.g. running classes
     * straight from an IDE).
     */
    public static String version() {
        return VERSION;
    }

    /** {@code sageactive4j/<version> (Java/<runtime version>)}. */
    public static String userAgent() {
        return NAME + "/" + VERSION + " (Java/" + System.getProperty("java.version") + ")";
    }

    private static String loadVersion() {
        try (InputStream in = SageActive4j.class.getResourceAsStream("version.properties")) {
            if (in == null) {
                return UNKNOWN_VERSION;
            }
            Properties props = new Properties();
            props.load(in);
            String version = props.getProperty("version");
            if (version == null || version.trim().isEmpty() || version.startsWith("@")) {
                return UNKNOWN_VERSION;
            }
            return version.trim();
        } catch (IOException e) {
            return UNKNOWN_VERSION;
        }
    }
}
