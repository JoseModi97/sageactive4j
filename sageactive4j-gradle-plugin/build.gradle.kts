plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.0"
}

group = "io.github.josemodi97"
// The sageactive4j version this plugin is released with, and the version
// of the reactor artifacts its tests compile the generated examples
// against. Pass -PsageActiveVersion=X.Y.Z (the same property the root
// build uses); the fallback must match the root pom.xml's <version>.
val sageActiveVersion = project.findProperty("sageActiveVersion") as String? ?: "0.1.0"
version = sageActiveVersion

description = "Gradle plugin for sageactive4j: detects your project's framework (plain Java, " +
        "javax/jakarta servlet, Spring Boot 2/3) and scaffolds a working Sage Active example " +
        "(./gradlew sageactive4jInit)."

repositories {
    mavenCentral()
    // The test classpath needs the reactor's own artifacts, which come from
    // `./gradlew publishToMavenLocal` (or `mvn install`) in the root build
    // until they're on Maven Central.
    mavenLocal()
}

// Detection, file writing and templates are shared, as source, with the
// Maven plugin, so the two plugins can't generate different code.
sourceSets {
    main {
        java.srcDir("../sageactive4j-scaffold/src/main/java")
        resources.srcDir("../sageactive4j-scaffold/src/main/resources")
    }
    test {
        java.srcDir("../sageactive4j-scaffold/src/test/java")
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addStringOption("Xdoclint:none", "-quiet")
    }
}

// The plugin runs inside whatever JVM runs the consumer's Gradle daemon;
// Java 8 bytecode keeps it usable everywhere sageactive4j is. Tests aren't
// shipped, and need Java 11 for the jakarta adapter on their classpath.
tasks.named<JavaCompile>("compileJava") {
    options.release.set(8)
    options.compilerArgs.add("-Xlint:-options")
}
tasks.named<JavaCompile>("compileTestJava") {
    options.release.set(11)
}

// The version the task suggests in "Add the dependency ...". A resource,
// not the jar manifest, so it's also there when TestKit loads the plugin
// from the classes directory.
val generateVersionResource = tasks.register("generateVersionResource") {
    val outputDir = layout.buildDirectory.dir("generated/version-resource")
    inputs.property("version", sageActiveVersion)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("io/github/josemodi97/sageactive4j/gradle/version.properties").asFile
        file.parentFile.mkdirs()
        file.writeText("version=$sageActiveVersion\n")
    }
}
sourceSets.main {
    resources.srcDir(generateVersionResource)
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Test-only: ScaffolderTest compiles the generated examples for real
    // against these. Spring Web 5.3 is Java 8 bytecode; the annotations used
    // are the same in Spring 6.
    testImplementation("io.github.josemodi97:sageactive4j-servlet:$sageActiveVersion")
    testImplementation("io.github.josemodi97:sageactive4j-jakarta:$sageActiveVersion")
    testImplementation("javax.servlet:javax.servlet-api:4.0.1")
    testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    testImplementation("org.springframework:spring-webmvc:5.3.31")
}

gradlePlugin {
    website.set("https://github.com/JoseModi97/sageactive4j")
    vcsUrl.set("https://github.com/JoseModi97/sageactive4j")
    plugins {
        create("sageactive4j") {
            id = "io.github.josemodi97.sageactive4j"
            implementationClass = "io.github.josemodi97.sageactive4j.gradle.SageActive4jPlugin"
            displayName = "sageactive4j"
            description = project.description
            tags.set(listOf("sage", "sage-active", "accounting", "graphql", "sdk", "scaffolding", "spring-boot"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
    // The TestKit tests resolve sageactive4j from mavenLocal too.
    systemProperty("sageActiveVersion", sageActiveVersion)
    testLogging {
        events("passed", "skipped", "failed")
    }
}
