plugins {
    `java-library`
    `maven-publish`
    signing
}

description = "sageactive4j auto-configuration for Spring Boot 3.x: a SageActive4jClient bean bound to " +
        "sageactive4j.* properties, an actuator health component, and opt-in browser sign-in endpoints."

val springBootVersion = "3.3.5"

dependencies {
    api(project(":sageactive4j-core"))
    api(project(":sageactive4j-jakarta"))
    implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    compileOnly("org.springframework:spring-webmvc:6.1.14")
    compileOnly("org.springframework.boot:spring-boot-actuator:$springBootVersion")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")

    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.assertj:assertj-core")
}

java {
    withJavadocJar()
    withSourcesJar()
}

// Spring Boot 3 itself requires Java 17.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.josemodi97.sageactive4j.spring.boot3")
    }
}

apply(from = "${rootDir}/gradle/publishing.gradle.kts")
