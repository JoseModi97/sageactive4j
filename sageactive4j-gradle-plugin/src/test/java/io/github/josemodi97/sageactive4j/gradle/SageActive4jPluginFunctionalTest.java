package io.github.josemodi97.sageactive4j.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Real consumer builds: apply the plugin, run {@code sageactive4jInit}, then
 * compile what it wrote against sageactive4j from {@code mavenLocal()}.
 */
class SageActive4jPluginFunctionalTest {

    private static final String VERSION = System.getProperty("sageActiveVersion");

    @TempDir
    Path projectDir;

    private void buildFile(String content) throws IOException {
        write("settings.gradle.kts", "rootProject.name = \"consumer\"\n");
        write("build.gradle.kts", content);
    }

    private void write(String path, String content) throws IOException {
        Path file = projectDir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private String read(String path) throws IOException {
        return new String(Files.readAllBytes(projectDir.resolve(path)), StandardCharsets.UTF_8);
    }

    private GradleRunner gradle(String... args) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(args)
                .forwardOutput();
    }

    @Test
    void plainProjectIsDetectedScaffoldedAndCompiles() throws IOException {
        buildFile("plugins {\n"
                + "    java\n"
                + "    id(\"io.github.josemodi97.sageactive4j\")\n"
                + "}\n"
                + "repositories { mavenLocal(); mavenCentral() }\n"
                + "tasks.withType<JavaCompile>().configureEach { options.release.set(8) }\n"
                + "dependencies { implementation(\"io.github.josemodi97:sageactive4j-core:" + VERSION + "\") }\n");

        // The configuration cache must work too: the task reads nothing from
        // the Project while it runs.
        BuildResult init = gradle("sageactive4jInit", "--configuration-cache").build();

        assertEquals(TaskOutcome.SUCCESS, init.task(":sageactive4jInit").getOutcome());
        assertTrue(init.getOutput().contains("Detected framework 'plain'"), init.getOutput());
        assertFalse(init.getOutput().contains("Add the dependency"), "core is already declared");
        assertTrue(Files.exists(projectDir.resolve("src/main/java/sageactive4j/SageActiveExample.java")));

        BuildResult compile = gradle("compileJava").build();
        assertEquals(TaskOutcome.SUCCESS, compile.task(":compileJava").getOutcome());
        assertTrue(Files.exists(projectDir.resolve("build/classes/java/main/sageactive4j/SageActiveExample.class")));
    }

    @Test
    void springBoot3IsDetectedFromTheBootPluginScaffoldedAndCompiles() throws IOException {
        buildFile("plugins {\n"
                + "    java\n"
                + "    id(\"org.springframework.boot\") version \"3.3.5\"\n"
                + "    id(\"io.spring.dependency-management\") version \"1.1.6\"\n"
                + "    id(\"io.github.josemodi97.sageactive4j\")\n"
                + "}\n"
                + "repositories { mavenLocal(); mavenCentral() }\n"
                + "dependencies {\n"
                + "    implementation(\"org.springframework.boot:spring-boot-starter-web\")\n"
                + "}\n");

        BuildResult init = gradle("sageactive4jInit").build();

        // The starter carries no version (the Boot plugin's BOM manages it),
        // so this proves detection reads the plugin's own version.
        assertTrue(init.getOutput().contains("Detected framework 'spring-boot3' "
                + "(found org.springframework.boot:spring-boot-gradle-plugin:3.3.5)"), init.getOutput());
        assertTrue(init.getOutput().contains(
                "Add the dependency io.github.josemodi97:sageactive4j-spring-boot3-starter:" + VERSION + "."),
                init.getOutput());
        assertTrue(read("src/main/resources/sageactive4j.properties").contains("sageactive4j.oauth.enabled=true"));

        // Follow the plugin's advice, then compile.
        buildFile(read("build.gradle.kts").replace("dependencies {\n", "dependencies {\n"
                + "    implementation(\"io.github.josemodi97:sageactive4j-spring-boot3-starter:" + VERSION + "\")\n"));
        BuildResult compile = gradle("compileJava").build();
        assertEquals(TaskOutcome.SUCCESS, compile.task(":compileJava").getOutcome());
    }

    @Test
    void explicitFrameworkAndForceFromTheCommandLine() throws IOException {
        buildFile("plugins { id(\"io.github.josemodi97.sageactive4j\") }\n");
        write("src/main/java/sageactive4j/SageActiveServlet.java", "// mine\n");

        BuildResult kept = gradle("sageactive4jInit", "--framework=servlet").build();
        assertEquals("// mine\n", read("src/main/java/sageactive4j/SageActiveServlet.java"));
        assertTrue(kept.getOutput().contains("left untouched"), kept.getOutput());
        assertFalse(kept.getOutput().contains("Detected framework"), kept.getOutput());

        // Not UP-TO-DATE on the second run: the task never tracks state.
        BuildResult forced = gradle("sageactive4jInit", "--framework=servlet", "--force").build();
        assertEquals(TaskOutcome.SUCCESS, forced.task(":sageactive4jInit").getOutcome());
        assertTrue(read("src/main/java/sageactive4j/SageActiveServlet.java").contains("import javax.servlet."));
    }

    @Test
    void anUnsupportedFrameworkFailsWithTheSupportedValues() throws IOException {
        buildFile("plugins { id(\"io.github.josemodi97.sageactive4j\") }\n");

        BuildResult result = gradle("sageactive4jInit", "--framework=quarkus").buildAndFail();

        assertTrue(result.getOutput().contains("Unsupported framework 'quarkus'. Supported values: auto, plain, "
                + "servlet, jakarta, spring-boot2, spring-boot3"), result.getOutput());
    }
}
