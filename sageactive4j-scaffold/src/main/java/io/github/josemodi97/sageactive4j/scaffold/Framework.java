package io.github.josemodi97.sageactive4j.scaffold;

import java.util.Locale;

/**
 * The project types the {@code init} goal / {@code sageactive4jInit} task can
 * scaffold for, each with the sageactive4j artifact its example needs.
 */
public enum Framework {

    PLAIN("plain", "sageactive4j-core"),
    SERVLET("servlet", "sageactive4j-servlet"),
    JAKARTA("jakarta", "sageactive4j-jakarta"),
    SPRING_BOOT2("spring-boot2", "sageactive4j-spring-boot2-starter"),
    SPRING_BOOT3("spring-boot3", "sageactive4j-spring-boot3-starter");

    public static final String GROUP_ID = "io.github.josemodi97";

    private final String id;
    private final String artifactId;

    Framework(String id, String artifactId) {
        this.id = id;
        this.artifactId = artifactId;
    }

    /** The value users pass on the command line, e.g. {@code spring-boot3}. */
    public String id() {
        return id;
    }

    /** The sageactive4j artifact the generated example compiles against. */
    public String artifactId() {
        return artifactId;
    }

    public boolean isSpringBoot() {
        return this == SPRING_BOOT2 || this == SPRING_BOOT3;
    }

    /** @throws IllegalArgumentException naming the supported values */
    public static Framework fromId(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (Framework framework : values()) {
            if (framework.id.equals(normalized)) {
                return framework;
            }
        }
        throw new IllegalArgumentException("Unsupported framework '" + value + "'. Supported values: auto, "
                + supportedIds());
    }

    public static String supportedIds() {
        StringBuilder ids = new StringBuilder();
        for (Framework framework : values()) {
            if (ids.length() > 0) {
                ids.append(", ");
            }
            ids.append(framework.id);
        }
        return ids.toString();
    }
}
