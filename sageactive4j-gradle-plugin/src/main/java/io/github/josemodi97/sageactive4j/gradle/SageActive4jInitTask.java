package io.github.josemodi97.sageactive4j.gradle;

import io.github.josemodi97.sageactive4j.scaffold.Framework;
import io.github.josemodi97.sageactive4j.scaffold.FrameworkDetector;
import io.github.josemodi97.sageactive4j.scaffold.FrameworkDetector.Coordinate;
import io.github.josemodi97.sageactive4j.scaffold.Scaffolder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

/**
 * Scaffolds a working Sage Active example for the project's framework. With
 * the default {@code auto}, the framework is detected from the declared
 * dependencies and the Spring Boot plugin, and the guess is logged. Existing
 * files are left untouched unless {@code --force}.
 *
 * <pre>
 * ./gradlew sageactive4jInit
 * ./gradlew sageactive4jInit --framework=spring-boot2 --force
 * </pre>
 */
public abstract class SageActive4jInitTask extends DefaultTask {

    @Inject
    public SageActive4jInitTask(ProjectLayout layout) {
        getFramework().convention("auto");
        getForce().convention(false);
        getJavaSourceDirectory().convention(layout.getProjectDirectory().dir("src/main/java"));
        getResourceDirectory().convention(layout.getProjectDirectory().dir("src/main/resources"));
        // It writes into the source tree, and only what's missing: there is
        // no output to be up to date with.
        doNotTrackState("Scaffolds source files; always runs");
    }

    /**
     * {@code auto} (default), {@code plain}, {@code servlet} ({@code javax.servlet}),
     * {@code jakarta}, {@code spring-boot2} or {@code spring-boot3}.
     */
    @Input
    @Option(option = "framework", description = "auto (default), plain, servlet, jakarta, spring-boot2 or spring-boot3")
    public abstract Property<String> getFramework();

    /** Overwrite files that already exist. */
    @Input
    @Option(option = "force", description = "Overwrite files that already exist")
    public abstract Property<Boolean> getForce();

    /** Where the example class is written (under a {@code sageactive4j} package). */
    @Internal
    public abstract DirectoryProperty getJavaSourceDirectory();

    /** Where {@code sageactive4j.properties} is written (Spring Boot only). */
    @Internal
    public abstract DirectoryProperty getResourceDirectory();

    /** {@code group:name[:version]} of what the project declares; set by the plugin. */
    @Input
    public abstract ListProperty<String> getDeclaredDependencies();

    @TaskAction
    public void run() throws IOException {
        List<Coordinate> declared = new ArrayList<>();
        for (String encoded : getDeclaredDependencies().get()) {
            declared.add(decode(encoded));
        }

        String requested = getFramework().get().trim();
        Framework target;
        if (requested.isEmpty() || "auto".equalsIgnoreCase(requested)) {
            FrameworkDetector.Detection detection = FrameworkDetector.detect(declared);
            target = detection.framework;
            getLogger().lifecycle("Detected framework '{}' ({}). Pass --framework=<value> to choose another.",
                    target.id(), detection.reason);
        } else {
            try {
                target = Framework.fromId(requested);
            } catch (IllegalArgumentException e) {
                throw new GradleException(e.getMessage());
            }
        }

        Scaffolder.write(Scaffolder.filesFor(target,
                        getJavaSourceDirectory().get().getAsFile().toPath(),
                        getResourceDirectory().get().getAsFile().toPath(),
                        "sageactive4jInit"),
                getForce().get(), getLogger()::lifecycle);

        getLogger().lifecycle("Next steps:");
        for (String step : Scaffolder.nextSteps(target, pluginVersion(),
                FrameworkDetector.declaresArtifact(declared, target))) {
            getLogger().lifecycle("  - {}", step);
        }
    }

    /** The sageactive4j version this plugin was built with, or {@code null}. */
    static String pluginVersion() {
        try (InputStream in = SageActive4jInitTask.class.getResourceAsStream("version.properties")) {
            if (in == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("version");
        } catch (IOException e) {
            return null;
        }
    }

    static String encode(Coordinate c) {
        return c.toString();
    }

    static Coordinate decode(String encoded) {
        String[] parts = encoded.split(":", 3);
        return new Coordinate(parts[0], parts.length > 1 ? parts[1] : "", parts.length > 2 ? parts[2] : null);
    }
}
