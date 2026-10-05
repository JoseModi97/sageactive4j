plugins {
    `java-library`
    `maven-publish`
    signing
}

description = "sageactive4j adapter for javax.servlet (Tomcat 8/9, Spring Boot 2, Java EE): " +
        "a secure SBC Auth sign-in flow (session-bound single-use state, PKCE) and callback handler."

dependencies {
    api(project(":sageactive4j-core"))
    compileOnly("javax.servlet:javax.servlet-api:4.0.1")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("javax.servlet:javax.servlet-api:4.0.1")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

java {
    withJavadocJar()
    withSourcesJar()
}

// Main code stays on the Java 8 floor; the tests use Mockito 5, which needs Java 11.
tasks.named<JavaCompile>("compileTestJava") {
    options.release.set(11)
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.josemodi97.sageactive4j.servlet")
    }
}

apply(from = "${rootDir}/gradle/publishing.gradle.kts")
