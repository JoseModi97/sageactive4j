package io.github.josemodi97.sageactive4j.scaffold;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Writes the example files for a {@link Framework}. Shared, as source, by the
 * Maven plugin and the standalone Gradle plugin build, so the two can't
 * generate different code.
 *
 * <ul>
 *   <li>{@code plain}: {@code sageactive4j/SageActiveExample.java}, a
 *       {@code main} that prints the user profile and the first customers.</li>
 *   <li>{@code servlet} / {@code jakarta}: {@code sageactive4j/SageActiveServlet.java},
 *       browser sign-in at {@code /sage/login} and {@code /sage/callback}.</li>
 *   <li>{@code spring-boot2} / {@code spring-boot3}: {@code sageactive4j/SageController.java}
 *       ({@code /sage/accounting/accounts}, {@code /sage/sales/invoice}) plus
 *       {@code sageactive4j.properties}, which it loads.</li>
 * </ul>
 *
 * Existing files are skipped unless {@code force} is set.
 */
public final class Scaffolder {

    /** Where the plugin's log output goes. */
    public interface Log {
        void info(String message);
    }

    public static final class GeneratedFile {
        public final Path path;
        public final String content;

        GeneratedFile(Path path, String content) {
            this.path = path;
            this.content = content;
        }
    }

    public static final String PROPERTIES_FILE = "sageactive4j.properties";

    private static final String TEMPLATES = "/io/github/josemodi97/sageactive4j/scaffold/templates/";

    private Scaffolder() {
    }

    /**
     * The files {@code framework} gets, rendered.
     *
     * @param generator how the user ran the generator, quoted in the file
     *                  header (e.g. {@code sageactive4j:init})
     */
    public static List<GeneratedFile> filesFor(Framework framework, Path javaSourceDir, Path resourceDir,
                                               String generator) {
        List<GeneratedFile> files = new ArrayList<>();
        switch (framework) {
            case PLAIN:
                files.add(java(javaSourceDir, "SageActiveExample", framework, generator));
                break;
            case SERVLET:
            case JAKARTA:
                files.add(java(javaSourceDir, "SageActiveServlet", framework, generator));
                break;
            case SPRING_BOOT2:
            case SPRING_BOOT3:
                files.add(java(javaSourceDir, "SageController", framework, generator));
                files.add(new GeneratedFile(resourceDir.resolve(PROPERTIES_FILE),
                        render("sageactive4j.properties.tmpl", framework, generator)));
                break;
            default:
                throw new IllegalStateException(framework.toString());
        }
        return Collections.unmodifiableList(files);
    }

    /**
     * Writes {@code files}, skipping (and logging) any that exist unless
     * {@code force} is set.
     *
     * @return the files actually written
     */
    public static List<Path> write(List<GeneratedFile> files, boolean force, Log log) throws IOException {
        List<Path> written = new ArrayList<>();
        for (GeneratedFile file : files) {
            boolean exists = Files.exists(file.path);
            if (exists && !force) {
                log.info(file.path + " already exists - left untouched (force to overwrite).");
                continue;
            }
            Files.createDirectories(file.path.getParent());
            Files.write(file.path, file.content.getBytes(StandardCharsets.UTF_8));
            log.info((exists ? "Overwrote " : "Wrote ") + file.path);
            written.add(file.path);
        }
        return written;
    }

    /**
     * What the user still has to do, one line per step.
     *
     * @param version          the sageactive4j version to suggest, or {@code null}
     * @param artifactDeclared whether the project already declares the needed artifact
     */
    public static List<String> nextSteps(Framework framework, String version, boolean artifactDeclared) {
        List<String> steps = new ArrayList<>();
        if (!artifactDeclared) {
            steps.add("Add the dependency " + Framework.GROUP_ID + ":" + framework.artifactId() + ":"
                    + (version == null ? "<version>" : version) + ".");
        }
        switch (framework) {
            case PLAIN:
                steps.add("Set the SAGEACTIVE4J_* environment variables listed at the top of "
                        + "SageActiveExample.java, then run sageactive4j.SageActiveExample.");
                break;
            case SERVLET:
            case JAKARTA:
                steps.add("Set the SAGEACTIVE4J_* environment variables listed at the top of "
                        + "SageActiveServlet.java, and register the redirect URI for your app in the "
                        + "Sage Developer Center.");
                steps.add("Deploy, then open /sage/login to sign in. Protect that path: whoever signs in "
                        + "sets the credentials the app uses.");
                break;
            default:
                steps.add("Move SageController into your application's package, under the "
                        + "@SpringBootApplication class, so component scanning finds it.");
                steps.add("Set the SAGEACTIVE4J_* environment variables used in " + PROPERTIES_FILE
                        + ", and register the redirect URI for your app in the Sage Developer Center.");
                steps.add("Start the app, then open /sage/oauth/login to sign in. Protect that path: "
                        + "whoever signs in sets the credentials the app uses.");
                break;
        }
        steps.add("Docs: https://github.com/JoseModi97/sageactive4j#readme");
        return steps;
    }

    private static GeneratedFile java(Path javaSourceDir, String className, Framework framework, String generator) {
        return new GeneratedFile(javaSourceDir.resolve("sageactive4j").resolve(className + ".java"),
                render(className + ".java.tmpl", framework, generator));
    }

    static String render(String template, Framework framework, String generator) {
        boolean jakarta = framework == Framework.JAKARTA;
        return load(template)
                .replace("{{generator}}", generator)
                .replace("{{artifact}}", framework.artifactId())
                .replace("{{servletPackage}}", jakarta ? "jakarta.servlet" : "javax.servlet")
                .replace("{{adapterPackage}}", jakarta ? "jakarta" : "servlet");
    }

    private static String load(String template) {
        try (InputStream in = Scaffolder.class.getResourceAsStream(TEMPLATES + template)) {
            if (in == null) {
                throw new IllegalStateException("Missing scaffold template " + template);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, n);
            }
            // Templates are checked out with whatever line endings git chose.
            return new String(out.toByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read scaffold template " + template, e);
        }
    }
}
