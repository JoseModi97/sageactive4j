package io.github.josemodi97.sageactive4j.cli;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/**
 * Everything the CLI takes from its process - environment, home directory,
 * streams, the browser - so tests can substitute each.
 */
final class CliContext {

    /** Opens a URL in the user's browser; returns false if it couldn't. */
    interface BrowserOpener {
        boolean open(String url);
    }

    final Map<String, String> env;
    final BufferedReader in;
    final PrintStream out;
    final PrintStream err;
    final BrowserOpener browser;
    /** Whether input comes from a person (affects prompts and hidden input). */
    final boolean interactive;

    CliContext(Map<String, String> env, InputStream in, PrintStream out, PrintStream err, BrowserOpener browser,
               boolean interactive) {
        this.env = env == null ? Collections.<String, String>emptyMap() : env;
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = out;
        this.err = err;
        this.browser = browser;
        this.interactive = interactive;
    }

    static CliContext system() {
        return new CliContext(System.getenv(), System.in, System.out, System.err, CliContext::openWithOs,
                System.console() != null);
    }

    /** {@code --home}, else {@code SAGEACTIVE4J_HOME}, else {@code ~/.sageactive4j}. */
    Path home(String flag) {
        if (flag != null && !flag.trim().isEmpty()) {
            return Paths.get(flag.trim());
        }
        String fromEnv = env.get("SAGEACTIVE4J_HOME");
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return Paths.get(fromEnv.trim());
        }
        return Paths.get(System.getProperty("user.home"), ".sageactive4j");
    }

    /** Opens a URL with the OS's own launcher (no AWT, so it also works in a native image). */
    static boolean openWithOs(String url) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String[] command;
        if (os.contains("win")) {
            command = new String[] {"rundll32", "url.dll,FileProtocolHandler", url};
        } else if (os.contains("mac")) {
            command = new String[] {"open", url};
        } else {
            command = new String[] {"xdg-open", url};
        }
        try {
            new ProcessBuilder(command).redirectErrorStream(true).start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
