package io.github.josemodi97.sageactive4j.maven;

import io.github.josemodi97.sageactive4j.scaffold.Framework;
import io.github.josemodi97.sageactive4j.scaffold.FrameworkDetector;
import io.github.josemodi97.sageactive4j.scaffold.FrameworkDetector.Coordinate;
import io.github.josemodi97.sageactive4j.scaffold.Scaffolder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Scaffolds a working Sage Active example for the project's framework:
 *
 * <pre>
 * mvn io.github.josemodi97:sageactive4j-maven-plugin:init
 * mvn io.github.josemodi97:sageactive4j-maven-plugin:init -Dsageactive4j.framework=jakarta
 * </pre>
 *
 * <p>With the default {@code auto}, the framework is detected from the
 * project's dependencies, dependency management and parent POM, and the
 * guess is logged. Existing files are left untouched unless
 * {@code -Dsageactive4j.force=true}.
 */
@Mojo(name = "init", requiresProject = true, threadSafe = true)
public class SageActive4jInitMojo extends AbstractMojo {

    /**
     * {@code auto} (default), {@code plain}, {@code servlet} ({@code javax.servlet}),
     * {@code jakarta}, {@code spring-boot2} or {@code spring-boot3}.
     */
    @Parameter(property = "sageactive4j.framework", defaultValue = "auto")
    String framework;

    /** Overwrite files that already exist. */
    @Parameter(property = "sageactive4j.force", defaultValue = "false")
    boolean force;

    /** Where the example class is written (under a {@code sageactive4j} package). */
    @Parameter(property = "sageactive4j.javaSourceDirectory", defaultValue = "${project.build.sourceDirectory}")
    File javaSourceDirectory;

    /** Where {@code sageactive4j.properties} is written (Spring Boot only). */
    @Parameter(property = "sageactive4j.resourceDirectory", defaultValue = "${project.basedir}/src/main/resources")
    File resourceDirectory;

    @Parameter(defaultValue = "${plugin.version}", readonly = true)
    String pluginVersion;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Coordinate> declared = declaredCoordinates(project);
        Framework target;
        if (framework == null || framework.trim().isEmpty() || "auto".equalsIgnoreCase(framework.trim())) {
            FrameworkDetector.Detection detection = FrameworkDetector.detect(declared);
            target = detection.framework;
            getLog().info("Detected framework '" + target.id() + "' (" + detection.reason
                    + "). Pass -Dsageactive4j.framework=<value> to choose another.");
        } else {
            try {
                target = Framework.fromId(framework);
            } catch (IllegalArgumentException e) {
                throw new MojoFailureException(e.getMessage());
            }
        }

        try {
            Scaffolder.write(Scaffolder.filesFor(target, javaSourceDirectory.toPath(), resourceDirectory.toPath(),
                    "sageactive4j:init"), force, getLog()::info);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write the sageactive4j example: " + e.getMessage(), e);
        }

        getLog().info("Next steps:");
        for (String step : Scaffolder.nextSteps(target, pluginVersion,
                FrameworkDetector.declaresArtifact(declared, target))) {
            getLog().info("  - " + step);
        }
    }

    /**
     * Dependencies (with managed versions filled in), managed dependencies
     * (a Spring Boot BOM import) and the parent POM. Nothing is resolved.
     */
    static List<Coordinate> declaredCoordinates(MavenProject project) {
        List<Coordinate> declared = new ArrayList<>();
        for (Dependency d : project.getDependencies()) {
            declared.add(new Coordinate(d.getGroupId(), d.getArtifactId(), d.getVersion()));
        }
        DependencyManagement management = project.getModel().getDependencyManagement();
        if (management != null) {
            for (Dependency d : management.getDependencies()) {
                declared.add(new Coordinate(d.getGroupId(), d.getArtifactId(), d.getVersion()));
            }
        }
        Parent parent = project.getModel().getParent();
        if (parent != null) {
            declared.add(new Coordinate(parent.getGroupId(), parent.getArtifactId(), parent.getVersion()));
        }
        return declared;
    }
}
