plugins {
    `java-library`
    `maven-publish`
    signing
}

description = "sageactive4j - a dependency-free Java SDK for the Sage Active Public API V2 (GraphQL): " +
        "SBC Auth OAuth 2.0, rate-limit backoff, multipart uploads, and typed accounting, sales, " +
        "purchases, banking, and analytics clients. Java 8+."

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withJavadocJar()
    withSourcesJar()
}

// Same @project.version@ token Maven's resource filtering replaces.
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("**/version.properties") {
        filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to mapOf("project.version" to project.version.toString())
        )
    }
}

// --- Multi-release jar (PLAN.md §3): a real module-info.java for Java 9+,
// and a Java 11+ layer (HttpClient-based transport from Phase 1 onwards)
// on top of the Java-8-only base classes.
sourceSets {
    create("java9") {
        java {
            srcDir("src/main/java9")
        }
    }
    create("java11") {
        java {
            srcDir("src/main/java11")
        }
    }
}

dependencies {
    "java9Implementation"(files(sourceSets["main"].output))
    "java11Implementation"(files(sourceSets["main"].output))
}

// A module-info.java compiled separately from the packages it describes needs
// its `exports` clauses validated against those packages as if they belonged
// to this compilation - that's what --patch-module does.
val moduleName = "io.github.josemodi97.sageactive4j.core"

tasks.named<JavaCompile>("compileJava9Java") {
    options.release.set(9)
    dependsOn(tasks.named("compileJava"))
    doFirst {
        options.compilerArgs.addAll(listOf("--patch-module", "$moduleName=${sourceSets["main"].output.asPath}"))
    }
}

tasks.named<JavaCompile>("compileJava11Java") {
    options.release.set(11)
    dependsOn(tasks.named("compileJava"))
    doFirst {
        options.compilerArgs.addAll(listOf("--patch-module", "$moduleName=${sourceSets["main"].output.asPath}"))
    }
}

tasks.jar {
    dependsOn("compileJava9Java", "compileJava11Java")

    into("META-INF/versions/9") {
        from(sourceSets["java9"].output)
    }
    into("META-INF/versions/11") {
        from(sourceSets["java11"].output)
    }

    manifest {
        attributes(
            "Multi-Release" to "true",
            "Implementation-Title" to "sageactive4j",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "sageactive4j contributors"
        )
    }
}

// Unit tests run against the unpackaged classes, which only contain the
// Java 8 HttpURLConnection transport. This re-runs the whole suite against
// the built multi-release jar, where a Java 11+ JVM selects the HttpClient
// variant - so both transports are proven by the same stub-server tests.
val testJar by tasks.registering(Test::class) {
    description = "Runs the test suite against the built multi-release jar."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = files(tasks.jar) + sourceSets["test"].output + configurations["testRuntimeClasspath"]
    systemProperty("sageactive4j.test.fromJar", "true")
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(testJar)
}

apply(from = "${rootDir}/gradle/publishing.gradle.kts")
