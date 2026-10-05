package io.github.josemodi97.sageactive4j.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Test;

class ProfileCommandsTest extends CliTestSupport {

    @Test
    void envWithNoProfilesExplainsWhatToDo() {
        Run r = run("env");
        assertEquals(0, r.exitCode, r.toString());
        assertTrue(r.out.contains("sageactive4j init"), r.out);
    }

    @Test
    void envListsProfilesAndSwitches() {
        profile("sandbox", "sandbox", "true", "region", "ES");
        profile("production", "organization-id", "org-9");

        Run list = run("env");
        assertTrue(list.out.contains("*  sandbox"), list.out);
        assertTrue(list.out.contains("production") && list.out.contains("org-9"), list.out);

        assertEquals(0, run("env", "production").exitCode);
        assertEquals("production", ConfigFile.load(home).activeProfile());
        assertTrue(run("env").out.contains("*  production"));
    }

    @Test
    void switchingToAMissingProfileFails() {
        Run r = run("env", "nope");
        assertEquals(1, r.exitCode);
        assertTrue(r.err.contains("init --profile nope"), r.err);
    }

    @Test
    void scriptedInitFromFlags() {
        Run r = runWithInput("\n", "init", "--profile", "ci", "--region", "de", "--subscription-key", "sk", "--client-id", "cid",
                "--redirect-uri", "http://127.0.0.1:9999/cb", "--sandbox", "--no-login");

        assertEquals(0, r.exitCode, r.toString());
        ConfigFile file = ConfigFile.load(home);
        assertEquals("ci", file.activeProfile());
        assertEquals("DE", file.get("ci", "region"));
        assertEquals("sk", file.get("ci", "subscription-key"));
        assertEquals("http://127.0.0.1:9999/cb", file.get("ci", "redirect-uri"));
        assertEquals("true", file.get("ci", "sandbox"));
        assertNull(file.get("ci", "client-secret"));
        assertFalse(r.out.contains("client secret is stored"));
    }

    @Test
    void interactiveInitAnswersPrompts() {
        String answers = String.join("\n", "work", "es", "the-key", "the-client", "the-secret", "", "y", "n") + "\n";
        Run r = runWithInput(answers, "init");

        assertEquals(0, r.exitCode, r.toString());
        ConfigFile file = ConfigFile.load(home);
        assertEquals("work", file.activeProfile());
        assertEquals("ES", file.get("work", "region"));
        assertEquals("the-key", file.get("work", "subscription-key"));
        assertEquals("the-secret", file.get("work", "client-secret"));
        assertEquals(InitCommand.DEFAULT_REDIRECT_URI, file.get("work", "redirect-uri"));
        assertEquals("true", file.get("work", "sandbox"));
        assertTrue(r.out.contains("client secret is stored"), r.out);
        assertTrue(r.out.contains("sageactive4j login"), r.out);
    }

    @Test
    void initRequiresASubscriptionKey() {
        Run r = runWithInput("\n\n\n\n", "init", "--profile", "x", "--region", "fr");
        assertEquals(1, r.exitCode);
        assertTrue(r.err.contains("subscription key is required"), r.err);
        assertFalse(Files.exists(home.resolve(ConfigFile.FILE_NAME)));
    }

    @Test
    void reRunningInitKeepsExistingSecretsOnEmptyAnswers() {
        profile("p", "client-secret", "old-secret");
        Run r = runWithInput("\n", "init", "--profile", "p", "--region", "fr", "--client-id", "cid", "--no-sandbox", "--no-login");
        // subscription key prompt answered empty -> keep; secret prompt -> keep
        assertEquals(0, r.exitCode, r.toString());
        ConfigFile file = ConfigFile.load(home);
        assertEquals("profile-key", file.get("p", "subscription-key"));
        assertEquals("old-secret", file.get("p", "client-secret"));
        assertEquals("false", file.get("p", "sandbox"));
    }

    @Test
    void configFileIsOwnerOnlyOnPosix() throws Exception {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        profile("p");
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(home.resolve(ConfigFile.FILE_NAME))));
    }

    @Test
    void invalidProfileNamesAreRejected() {
        Run r = run("env", "../../etc");
        assertEquals(1, r.exitCode);
        assertTrue(r.err.contains("Profile names"), r.err);
    }

    @Test
    void versionShowsTheSdkVersion() {
        Run r = run("--version");
        assertEquals(0, r.exitCode);
        assertTrue(r.out.startsWith("sageactive4j CLI "), r.out);
    }
}
