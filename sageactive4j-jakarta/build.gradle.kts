plugins {
    `java-library`
    `maven-publish`
    signing
}

description = "sageactive4j adapter for jakarta.servlet (Tomcat 10+, Spring Boot 3, Jakarta EE 9+): " +
        "a secure SBC Auth sign-in flow (session-bound single-use state, PKCE) and callback handler."

dependencies {
    api(project(":sageactive4j-core"))
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

java {
    withJavadocJar()
    withSourcesJar()
}

// Jakarta Servlet 6 itself requires Java 11.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.josemodi97.sageactive4j.jakarta")
    }
}

apply(from = "${rootDir}/gradle/publishing.gradle.kts")
