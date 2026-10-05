plugins {
    java
}

allprojects {
    group = "io.github.josemodi97"
    // Fallback must be kept in sync with the root pom.xml's <version> -
    // the Gradle and Maven builds compile the same source tree independently.
    version = project.findProperty("sageActiveVersion") as String? ?: "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    // sageactive4j-bom is a java-platform (BOM) project, which cannot carry
    // the java/java-library plugin, so it opts out of this shared block.
    if (name == "sageactive4j-bom") {
        return@subprojects
    }

    apply(plugin = "java")

    tasks.withType<JavaCompile>().configureEach {
        // Compile against the Java 8 API surface regardless of which JDK
        // runs the build, so the published jar stays usable on Java 8+.
        // Modules with a higher framework-imposed floor override this.
        options.release.set(8)
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:-options")
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            encoding = "UTF-8"
            charSet = "UTF-8"
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
