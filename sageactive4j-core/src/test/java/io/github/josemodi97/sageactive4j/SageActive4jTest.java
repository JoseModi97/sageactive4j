package io.github.josemodi97.sageactive4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SageActive4jTest {

    @Test
    void versionIsFilledInByTheBuild() {
        // Both builds filter version.properties before tests run, so an
        // unreplaced token (reported as "dev") means the filtering broke.
        String version = SageActive4j.version();
        assertNotNull(version);
        assertFalse(version.isEmpty());
        assertNotEquals("dev", version);
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+.*"), "unexpected version: " + version);
    }

    @Test
    void userAgentNamesLibraryVersionAndRuntime() {
        String expected = "sageactive4j/" + SageActive4j.version()
                + " (Java/" + System.getProperty("java.version") + ")";
        assertEquals(expected, SageActive4j.userAgent());
    }
}
