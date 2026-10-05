package io.github.josemodi97.sageactive4j.gradle;

import io.github.josemodi97.sageactive4j.scaffold.FrameworkDetector.Coordinate;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

/**
 * Registers {@code sageactive4jInit}:
 *
 * <pre>{@code
 * plugins {
 *     id("io.github.josemodi97.sageactive4j") version "0.1.0"
 * }
 * }</pre>
 *
 * then {@code ./gradlew sageactive4jInit}, optionally with
 * {@code --framework=jakarta} and {@code --force}.
 */
public class SageActive4jPlugin implements Plugin<Project> {

    private static final String SPRING_BOOT_PLUGIN = "org.springframework.boot";

    @Override
    public void apply(Project project) {
        project.getTasks().register("sageactive4jInit", SageActive4jInitTask.class, task -> {
            task.setGroup("sageactive4j");
            task.setDescription("Scaffolds a working Sage Active example for this project's framework.");
            // Read when the task runs, after the build script's
            // dependencies { } block has been evaluated.
            task.getDeclaredDependencies().set(project.provider(() -> declaredCoordinates(project)));
        });
    }

    /**
     * Every configuration's declared dependencies, plus the Spring Boot
     * Gradle plugin and its version if applied (its BOM leaves the starters
     * themselves without one). Nothing is resolved.
     */
    static List<String> declaredCoordinates(Project project) {
        List<String> declared = new ArrayList<>();
        for (Configuration configuration : project.getConfigurations()) {
            for (Dependency d : configuration.getDependencies()) {
                if (d.getGroup() != null) {
                    declared.add(SageActive4jInitTask.encode(new Coordinate(d.getGroup(), d.getName(), d.getVersion())));
                }
            }
        }
        if (project.getPluginManager().hasPlugin(SPRING_BOOT_PLUGIN)) {
            Object bootPlugin = project.getPlugins().findPlugin(SPRING_BOOT_PLUGIN);
            String version = bootPlugin == null ? null : bootPlugin.getClass().getPackage().getImplementationVersion();
            declared.add(SageActive4jInitTask.encode(
                    new Coordinate(SPRING_BOOT_PLUGIN, "spring-boot-gradle-plugin", version)));
        }
        return declared;
    }
}
