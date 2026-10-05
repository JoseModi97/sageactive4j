package io.github.josemodi97.sageactive4j.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

/** Runs the real picocli command line with captured streams, a temp home and a fake Sage. */
abstract class CliTestSupport {

    @TempDir
    Path home;

    FakeSage sage;
    final Map<String, String> env = new HashMap<>();
    CliContext.BrowserOpener browser = url -> false;

    /** Result of one CLI run. */
    static final class Run {
        final int exitCode;
        final String out;
        final String err;

        Run(int exitCode, String out, String err) {
            this.exitCode = exitCode;
            this.out = out;
            this.err = err;
        }

        @Override
        public String toString() {
            return "exit " + exitCode + "\n--- out\n" + out + "--- err\n" + err;
        }
    }

    @BeforeEach
    void startSage() throws Exception {
        sage = new FakeSage();
        env.put("SAGEACTIVE4J_HOME", home.toString());
    }

    @AfterEach
    void stopSage() {
        sage.close();
    }

    Run runWithInput(String stdin, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        CliContext context = new CliContext(env,
                new ByteArrayInputStream((stdin == null ? "" : stdin).getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true), new PrintStream(err, true), browser, false);
        int exit = SageActive4jCli.run(context, args);
        return new Run(exit, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    Run run(String... args) {
        return runWithInput(null, args);
    }

    /** A saved profile pointing at the fake Sage. */
    ConfigFile profile(String name, String... keyValues) {
        ConfigFile file = ConfigFile.load(home);
        file.set(name, "subscription-key", "profile-key");
        file.set(name, "base-url", sage.url());
        file.set(name, "token-url", sage.url() + "/connect/token");
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            file.set(name, keyValues[i], keyValues[i + 1]);
        }
        if (file.profiles().size() == 1) {
            file.setActiveProfile(name);
        }
        file.save();
        return ConfigFile.load(home);
    }
}
