package io.github.josemodi97.sageactive4j.scaffold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.scaffold.FrameworkDetector.Coordinate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FrameworkDetectorTest {

    private static Framework detect(Coordinate... declared) {
        return FrameworkDetector.detect(Arrays.asList(declared)).framework;
    }

    private static Coordinate c(String group, String artifact, String version) {
        return new Coordinate(group, artifact, version);
    }

    @Test
    void plainWhenNothingRelevantIsDeclared() {
        assertEquals(Framework.PLAIN, detect());
        assertEquals(Framework.PLAIN, detect(c("junit", "junit", "4.13.2")));
    }

    @Test
    void servletApis() {
        assertEquals(Framework.SERVLET, detect(c("javax.servlet", "javax.servlet-api", "4.0.1")));
        assertEquals(Framework.JAKARTA, detect(c("jakarta.servlet", "jakarta.servlet-api", "6.0.0")));
    }

    @Test
    void springBootMajorFromAnyVersionedBootCoordinate() {
        assertEquals(Framework.SPRING_BOOT2,
                detect(c("org.springframework.boot", "spring-boot-starter-web", "2.7.18")));
        assertEquals(Framework.SPRING_BOOT3,
                detect(c("org.springframework.boot", "spring-boot-starter-web", "3.3.5")));
        // Maven parent POM, BOM import, Gradle plugin: all just coordinates.
        assertEquals(Framework.SPRING_BOOT2,
                detect(c("org.springframework.boot", "spring-boot-starter-parent", "2.7.18")));
        assertEquals(Framework.SPRING_BOOT3,
                detect(c("org.springframework.boot", "spring-boot-dependencies", "3.4.1")));
    }

    @Test
    void aVersionlessStarterTakesTheVersionOfAnotherBootCoordinate() {
        assertEquals(Framework.SPRING_BOOT2, detect(
                c("org.springframework.boot", "spring-boot-starter-web", null),
                c("org.springframework.boot", "spring-boot-gradle-plugin", "2.7.18")));
    }

    @Test
    void servletNamespaceBreaksTheTieWhenNoBootVersionIsVisible() {
        assertEquals(Framework.SPRING_BOOT3, detect(
                c("org.springframework.boot", "spring-boot-starter-web", null),
                c("jakarta.servlet", "jakarta.servlet-api", "6.0.0")));
        assertEquals(Framework.SPRING_BOOT2, detect(
                c("org.springframework.boot", "spring-boot-starter-web", "${spring-boot.version}"),
                c("javax.servlet", "javax.servlet-api", "4.0.1")));
    }

    @Test
    void unknownBootVersionDefaultsToBoot3AndSaysSo() {
        FrameworkDetector.Detection detection = FrameworkDetector.detect(Collections.singletonList(
                c("org.springframework.boot", "spring-boot-starter-web", null)));
        assertEquals(Framework.SPRING_BOOT3, detection.framework);
        assertTrue(detection.reason.contains("assuming Spring Boot 3"), detection.reason);
    }

    @Test
    void newerBootMajorsGetTheBoot3StarterWithAWarning() {
        FrameworkDetector.Detection detection = FrameworkDetector.detect(Collections.singletonList(
                c("org.springframework.boot", "spring-boot-starter-parent", "4.0.0")));
        assertEquals(Framework.SPRING_BOOT3, detection.framework);
        assertTrue(detection.reason.contains("tested against Spring Boot 3.x"), detection.reason);
    }

    @Test
    void declaresArtifact() {
        List<Coordinate> declared = Arrays.asList(
                c("io.github.josemodi97", "sageactive4j-jakarta", "0.1.0"),
                c("jakarta.servlet", "jakarta.servlet-api", "6.0.0"));
        assertTrue(FrameworkDetector.declaresArtifact(declared, Framework.JAKARTA));
        assertFalse(FrameworkDetector.declaresArtifact(declared, Framework.SERVLET));
    }

    @Test
    void frameworkIdsAreCaseAndWhitespaceTolerantAndListedOnError() {
        assertEquals(Framework.SPRING_BOOT3, Framework.fromId(" Spring-Boot3 "));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Framework.fromId("quarkus"));
        assertTrue(e.getMessage().contains("auto, plain, servlet, jakarta, spring-boot2, spring-boot3"), e.getMessage());
    }
}
