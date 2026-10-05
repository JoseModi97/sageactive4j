package io.github.josemodi97.sageactive4j.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SageActive4jInitMojoTest {

    @TempDir
    Path projectDir;

    private final List<String> log = new ArrayList<>();
    private final Model model = new Model();

    private SageActive4jInitMojo mojo(String framework) {
        SageActive4jInitMojo mojo = new SageActive4jInitMojo();
        mojo.framework = framework;
        mojo.javaSourceDirectory = projectDir.resolve("src/main/java").toFile();
        mojo.resourceDirectory = projectDir.resolve("src/main/resources").toFile();
        mojo.pluginVersion = "9.9.9";
        mojo.project = new MavenProject(model);
        mojo.setLog(new SystemStreamLog() {
            @Override
            public void info(CharSequence message) {
                log.add(message.toString());
            }
        });
        return mojo;
    }

    private void declare(String group, String artifact, String version) {
        Dependency d = new Dependency();
        d.setGroupId(group);
        d.setArtifactId(artifact);
        d.setVersion(version);
        model.addDependency(d);
    }

    private boolean logged(String text) {
        for (String line : log) {
            if (line.contains(text)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void autoDetectsSpringBootFromTheParentPomAndWritesTheController() throws Exception {
        Parent parent = new Parent();
        parent.setGroupId("org.springframework.boot");
        parent.setArtifactId("spring-boot-starter-parent");
        parent.setVersion("2.7.18");
        model.setParent(parent);
        declare("org.springframework.boot", "spring-boot-starter-web", null);

        mojo("auto").execute();

        assertTrue(Files.exists(projectDir.resolve("src/main/java/sageactive4j/SageController.java")));
        assertTrue(Files.exists(projectDir.resolve("src/main/resources/sageactive4j.properties")));
        assertTrue(logged("Detected framework 'spring-boot2'"), log.toString());
        assertTrue(logged("Add the dependency io.github.josemodi97:sageactive4j-spring-boot2-starter:9.9.9."),
                log.toString());
    }

    @Test
    void autoFallsBackToPlain() throws Exception {
        mojo(null).execute();

        assertTrue(Files.exists(projectDir.resolve("src/main/java/sageactive4j/SageActiveExample.java")));
        assertTrue(logged("Detected framework 'plain'"), log.toString());
    }

    @Test
    void anExplicitFrameworkWinsAndADeclaredArtifactIsNotRequestedAgain() throws Exception {
        declare("io.github.josemodi97", "sageactive4j-jakarta", "0.1.0");

        mojo("Jakarta").execute();

        String servlet = new String(Files.readAllBytes(
                projectDir.resolve("src/main/java/sageactive4j/SageActiveServlet.java")), StandardCharsets.UTF_8);
        assertTrue(servlet.contains("import jakarta.servlet.http.HttpServlet;"));
        assertFalse(logged("Detected framework"), log.toString());
        assertFalse(logged("Add the dependency"), log.toString());
    }

    @Test
    void anUnsupportedFrameworkIsAUserFailure() {
        MojoFailureException e = assertThrows(MojoFailureException.class, () -> mojo("quarkus").execute());
        assertTrue(e.getMessage().contains("spring-boot3"), e.getMessage());
    }

    @Test
    void forceOverwritesAnExistingExample() throws Exception {
        Path example = projectDir.resolve("src/main/java/sageactive4j/SageActiveExample.java");
        Files.createDirectories(example.getParent());
        Files.write(example, "// mine\n".getBytes(StandardCharsets.UTF_8));

        mojo("plain").execute();
        assertEquals("// mine\n", read(example));

        SageActive4jInitMojo forced = mojo("plain");
        forced.force = true;
        forced.execute();
        assertTrue(read(example).contains("public class SageActiveExample"));
    }

    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
