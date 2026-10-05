package io.github.josemodi97.sageactive4j.scaffold;

import java.util.Collection;

/**
 * Guesses which {@link Framework} a project uses from the coordinates it
 * declares. Each build tool's plugin collects those coordinates without
 * resolving anything: Maven passes its dependencies, managed dependencies
 * (a Spring Boot BOM import) and parent POM; Gradle passes every
 * configuration's declared dependencies plus the Spring Boot Gradle plugin,
 * if applied, with its version.
 *
 * <p>A heuristic: anything unrecognised falls back to {@link Framework#PLAIN},
 * and the reason is always returned so the plugins can log their guess.
 */
public final class FrameworkDetector {

    private static final String SPRING_BOOT_GROUP = "org.springframework.boot";

    private FrameworkDetector() {
    }

    /** A declared {@code group:artifact:version}; the version may be {@code null}. */
    public static final class Coordinate {
        final String group;
        final String artifact;
        final String version;

        public Coordinate(String group, String artifact, String version) {
            this.group = group;
            this.artifact = artifact;
            this.version = version;
        }

        public boolean is(String group, String artifact) {
            return group.equals(this.group) && artifact.equals(this.artifact);
        }

        @Override
        public String toString() {
            return group + ":" + artifact + (version == null ? "" : ":" + version);
        }
    }

    public static final class Detection {
        public final Framework framework;
        public final String reason;

        Detection(Framework framework, String reason) {
            this.framework = framework;
            this.reason = reason;
        }
    }

    public static Detection detect(Collection<Coordinate> declared) {
        boolean jakartaServlet = false;
        boolean javaxServlet = false;
        Coordinate springBoot = null;
        String springBootMajor = null;

        for (Coordinate c : declared) {
            if (c.is("jakarta.servlet", "jakarta.servlet-api")) {
                jakartaServlet = true;
            } else if (c.is("javax.servlet", "javax.servlet-api")) {
                javaxServlet = true;
            } else if (SPRING_BOOT_GROUP.equals(c.group)) {
                String major = majorVersionOf(c.version);
                if (springBoot == null || (springBootMajor == null && major != null)) {
                    springBoot = c;
                    springBootMajor = major;
                }
            }
        }

        if (springBoot != null) {
            if ("2".equals(springBootMajor)) {
                return new Detection(Framework.SPRING_BOOT2, "found " + springBoot);
            }
            if ("3".equals(springBootMajor)) {
                return new Detection(Framework.SPRING_BOOT3, "found " + springBoot);
            }
            if (springBootMajor != null && springBootMajor.matches("\\d+") && Integer.parseInt(springBootMajor) > 3) {
                return new Detection(Framework.SPRING_BOOT3, "found " + springBoot
                        + "; the spring-boot3 starter is built and tested against Spring Boot 3.x");
            }
            // No visible version (managed elsewhere): Boot 3 uses the jakarta
            // servlet namespace and Boot 2 the javax one.
            if (jakartaServlet) {
                return new Detection(Framework.SPRING_BOOT3, "found " + springBoot + " with jakarta.servlet-api");
            }
            if (javaxServlet) {
                return new Detection(Framework.SPRING_BOOT2, "found " + springBoot + " with javax.servlet-api");
            }
            return new Detection(Framework.SPRING_BOOT3, "found " + springBoot
                    + " without a version; assuming Spring Boot 3");
        }
        if (jakartaServlet) {
            return new Detection(Framework.JAKARTA, "found jakarta.servlet-api");
        }
        if (javaxServlet) {
            return new Detection(Framework.SERVLET, "found javax.servlet-api");
        }
        return new Detection(Framework.PLAIN, "no Spring Boot or servlet API dependency found");
    }

    /** Whether the project already declares {@code framework}'s sageactive4j artifact. */
    public static boolean declaresArtifact(Collection<Coordinate> declared, Framework framework) {
        for (Coordinate c : declared) {
            if (c.is(Framework.GROUP_ID, framework.artifactId())) {
                return true;
            }
        }
        return false;
    }

    private static String majorVersionOf(String version) {
        if (version == null) {
            return null;
        }
        String trimmed = version.trim();
        // An unresolved property such as ${spring-boot.version} says nothing.
        if (trimmed.isEmpty() || trimmed.startsWith("$")) {
            return null;
        }
        int dot = trimmed.indexOf('.');
        return dot > 0 ? trimmed.substring(0, dot) : trimmed;
    }
}
