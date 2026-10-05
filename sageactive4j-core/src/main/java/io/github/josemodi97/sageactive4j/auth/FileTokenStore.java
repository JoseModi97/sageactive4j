package io.github.josemodi97.sageactive4j.auth;

import io.github.josemodi97.sageactive4j.exception.SageActive4jException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps tokens in a properties file, readable and writable only by its owner
 * where the file system supports POSIX permissions. Writes go to a temporary
 * file that is then moved into place, so a crash never leaves a half-written
 * token file behind.
 *
 * <p>Suited to a CLI or a single-instance service. The file holds live
 * credentials: keep it out of version control and backups you don't trust.
 */
public final class FileTokenStore implements TokenStore {

    private static final Logger LOG = Logger.getLogger(FileTokenStore.class.getName());

    private final Path file;

    public FileTokenStore(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    @Override
    public synchronized SageToken load() {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            throw new SageActive4jException("Could not read token file " + file + ": " + e.getMessage(), e);
        }
        String accessToken = props.getProperty("access_token");
        String refreshToken = props.getProperty("refresh_token");
        if (accessToken == null && refreshToken == null) {
            return null;
        }
        String expiresAt = props.getProperty("expires_at_epoch_millis");
        Long expiresAtMillis = null;
        if (expiresAt != null && !expiresAt.isEmpty()) {
            try {
                expiresAtMillis = Long.parseLong(expiresAt);
            } catch (NumberFormatException e) {
                expiresAtMillis = null;
            }
        }
        return new SageToken(accessToken, refreshToken, props.getProperty("token_type"),
                props.getProperty("scope"), expiresAtMillis);
    }

    @Override
    public synchronized void save(SageToken token) {
        if (token == null) {
            clear();
            return;
        }
        Properties props = new Properties();
        putIfNotNull(props, "access_token", token.getAccessToken());
        putIfNotNull(props, "refresh_token", token.getRefreshToken());
        putIfNotNull(props, "token_type", token.getTokenType());
        putIfNotNull(props, "scope", token.getScope());
        if (token.getExpiresAtEpochMillis() != null) {
            props.setProperty("expires_at_epoch_millis", token.getExpiresAtEpochMillis().toString());
        }

        Path tmp = null;
        try {
            Path dir = file.toAbsolutePath().getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            tmp = Files.createTempFile(dir, ".sageactive4j-token", ".tmp");
            restrictToOwner(tmp);
            try (OutputStream out = Files.newOutputStream(tmp)) {
                props.store(out, "sageactive4j tokens - keep private");
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            tmp = null;
        } catch (IOException e) {
            throw new SageActive4jException("Could not write token file " + file + ": " + e.getMessage(), e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // best effort
                }
            }
        }
    }

    @Override
    public synchronized void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new SageActive4jException("Could not delete token file " + file + ": " + e.getMessage(), e);
        }
    }

    private static void restrictToOwner(Path path) {
        try {
            Set<PosixFilePermission> ownerOnly = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(path, ownerOnly);
        } catch (UnsupportedOperationException e) {
            // Not a POSIX file system (e.g. Windows): fall back to the coarse java.io.File API.
            java.io.File f = path.toFile();
            boolean ok = f.setReadable(false, false) & f.setReadable(true, true)
                    & f.setWritable(false, false) & f.setWritable(true, true);
            if (!ok) {
                LOG.log(Level.WARNING, "Could not restrict permissions on token file {0}; "
                        + "make sure only you can read it", path);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not restrict permissions on token file {0}: {1}",
                    new Object[] {path, e.getMessage()});
        }
    }

    private static void putIfNotNull(Properties props, String key, String value) {
        if (value != null) {
            props.setProperty(key, value);
        }
    }
}
