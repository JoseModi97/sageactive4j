package io.github.josemodi97.sageactive4j.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTokenStoreTest {

    @TempDir
    Path dir;

    @Test
    void roundTrips() {
        FileTokenStore store = new FileTokenStore(dir.resolve("nested/tokens.properties"));
        assertNull(store.load());

        store.save(new SageToken("access", "refresh", "Bearer", "RDSA WDSA", 1_790_000_000_000L));
        SageToken loaded = new FileTokenStore(store.getFile()).load();

        assertEquals("access", loaded.getAccessToken());
        assertEquals("refresh", loaded.getRefreshToken());
        assertEquals("Bearer", loaded.getTokenType());
        assertEquals("RDSA WDSA", loaded.getScope());
        assertEquals(Long.valueOf(1_790_000_000_000L), loaded.getExpiresAtEpochMillis());
    }

    @Test
    void unknownExpiryRoundTripsAsNull() {
        FileTokenStore store = new FileTokenStore(dir.resolve("t.properties"));
        store.save(new SageToken("a", null, null, null, null));
        assertNull(store.load().getExpiresAtEpochMillis());
        assertNull(store.load().getRefreshToken());
    }

    @Test
    void clearDeletesTheFile() {
        FileTokenStore store = new FileTokenStore(dir.resolve("t.properties"));
        store.save(new SageToken("a", "r", null, null, null));
        store.clear();
        assertFalse(Files.exists(store.getFile()));
        assertNull(store.load());
        store.clear(); // idempotent
    }

    @Test
    void leavesNoTempFilesBehind() throws Exception {
        FileTokenStore store = new FileTokenStore(dir.resolve("t.properties"));
        store.save(new SageToken("a", "r", null, null, null));
        store.save(new SageToken("b", "r2", null, null, null));
        try (java.util.stream.Stream<Path> files = Files.list(dir)) {
            assertEquals(1, files.count());
        }
        assertEquals("b", store.load().getAccessToken());
    }

    @Test
    void isOwnerOnlyOnPosixFileSystems() throws Exception {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        FileTokenStore store = new FileTokenStore(dir.resolve("t.properties"));
        store.save(new SageToken("a", "r", null, null, null));
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(store.getFile())));
    }
}
