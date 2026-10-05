package io.github.josemodi97.sageactive4j.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Named credential profiles in {@code <home>/config.properties}:
 *
 * <pre>
 * active=sandbox
 * profile.sandbox.region=FR
 * profile.sandbox.subscription-key=...
 * profile.sandbox.sandbox=true
 * </pre>
 *
 * Written atomically with owner-only permissions where the file system
 * supports them; it may hold a client secret.
 */
final class ConfigFile {

    static final String FILE_NAME = "config.properties";
    static final String DEFAULT_PROFILE = "default";
    private static final Pattern PROFILE_NAME = Pattern.compile("[A-Za-z0-9_-]{1,40}");

    private final Path home;
    private final Properties props = new Properties();

    private ConfigFile(Path home) {
        this.home = home;
    }

    static ConfigFile load(Path home) {
        ConfigFile file = new ConfigFile(home);
        Path path = home.resolve(FILE_NAME);
        if (Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                file.props.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + path, e);
            }
        }
        return file;
    }

    static String validProfileName(String name) {
        if (name == null || !PROFILE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Profile names are 1-40 letters, digits, '-' or '_': " + name);
        }
        return name;
    }

    Path home() {
        return home;
    }

    Path path() {
        return home.resolve(FILE_NAME);
    }

    /** The active profile, {@value #DEFAULT_PROFILE} if none is set. */
    String activeProfile() {
        String active = props.getProperty("active");
        return active == null || active.trim().isEmpty() ? DEFAULT_PROFILE : active.trim();
    }

    void setActiveProfile(String name) {
        props.setProperty("active", validProfileName(name));
    }

    TreeSet<String> profiles() {
        TreeSet<String> names = new TreeSet<String>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("profile.")) {
                int dot = key.indexOf('.', "profile.".length());
                if (dot > 0) {
                    names.add(key.substring("profile.".length(), dot));
                }
            }
        }
        return names;
    }

    boolean hasProfile(String name) {
        return profiles().contains(name);
    }

    /** A profile setting, or {@code null}. */
    String get(String profile, String key) {
        String value = props.getProperty("profile." + profile + "." + key);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /** Sets (or, for {@code null}/blank, removes) a profile setting. */
    void set(String profile, String key, String value) {
        String name = "profile." + validProfileName(profile) + "." + key;
        if (value == null || value.trim().isEmpty()) {
            props.remove(name);
        } else {
            props.setProperty(name, value.trim());
        }
    }

    Map<String, String> settings(String profile) {
        Map<String, String> settings = new TreeMap<String, String>();
        String prefix = "profile." + profile + ".";
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                settings.put(key.substring(prefix.length()), props.getProperty(key));
            }
        }
        return settings;
    }

    /** The token file of a profile. */
    Path tokenFile(String profile) {
        return home.resolve("tokens-" + validProfileName(profile) + ".properties");
    }

    void save() {
        Path tmp = null;
        try {
            Files.createDirectories(home);
            tmp = Files.createTempFile(home, ".config", ".tmp");
            OwnerOnly.restrict(tmp);
            try (OutputStream out = Files.newOutputStream(tmp)) {
                props.store(out, "sageactive4j CLI profiles - may contain secrets, keep private");
            }
            try {
                Files.move(tmp, path(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path(), StandardCopyOption.REPLACE_EXISTING);
            }
            tmp = null;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + path(), e);
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

    /** Owner-only file permissions, best effort off POSIX. */
    static final class OwnerOnly {
        private OwnerOnly() {
        }

        static void restrict(Path path) {
            try {
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException | IOException e) {
                java.io.File f = path.toFile();
                f.setReadable(false, false);
                f.setReadable(true, true);
                f.setWritable(false, false);
                f.setWritable(true, true);
            }
        }
    }
}
