plugins {
    `java-library`
    `maven-publish`
    signing
}

description = "sageactive4j auto-configuration for Spring Boot 2.x: a SageActive4jClient bean bound to " +
        "sageactive4j.* properties, an actuator health component, and opt-in browser sign-in endpoints."

val springBootVersion = "2.7.18"

dependencies {
    api(project(":sageactive4j-core"))
    api(project(":sageactive4j-servlet"))
    implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    compileOnly("org.springframework:spring-webmvc:5.3.31")
    compileOnly("org.springframework.boot:spring-boot-actuator:$springBootVersion")
    compileOnly("javax.servlet:javax.servlet-api:4.0.1")
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:2.13.5")
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

// Main code keeps the Java 8 floor (Spring Boot 2 runs on 8); the tests use Java 11 APIs.
tasks.named<JavaCompile>("compileTestJava") {
    options.release.set(11)
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.josemodi97.sageactive4j.spring.boot2")
    }
}

apply(from = "${rootDir}/gradle/publishing.gradle.kts")
