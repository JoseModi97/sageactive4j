plugins {
    application
    id("com.gradleup.shadow") version "8.3.5"
    id("org.graalvm.buildtools.native") version "0.10.3"
    `maven-publish`
    signing
}

description = "sageactive4j command-line tool: set up profiles, sign in through the browser, check the setup, " +
        "pick an organization, run GraphQL queries and create sandbox invoices against Sage Active."

val mainClassName = "io.github.josemodi97.sageactive4j.cli.SageActive4jCli"

dependencies {
    implementation(project(":sageactive4j-core"))
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set(mainClassName)
    applicationName = "sageactive4j"
}

// picocli's annotation processor generates the GraalVM reflection config
// (META-INF/native-image/picocli-generated/...) that the @Command classes
// need for native-image compilation.
tasks.compileJava {
    options.compilerArgs.add("-Aproject=io.github.josemodi97/sageactive4j-cli")
}

// Main code keeps the Java 8 floor; the tests use Java 11 APIs.
tasks.named<JavaCompile>("compileTestJava") {
    options.release.set(11)
}

// Native compilation needs a GraalVM JDK (and MSVC on Windows); CI builds
// and smoke-tests the real binary. Only compilation is configured, not
// native test execution (which would need a Java 11+ main target).
graalvmNative {
    testSupport.set(false)
    binaries {
        named("main") {
            imageName.set("sageactive4j")
            mainClass.set(mainClassName)
            buildArgs.addAll("--no-fallback", "--enable-url-protocols=http,https")
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.shadowJar {
    manifest {
        // Keeps core's multi-release layer working inside the fat jar: Java 11+
        // runs the HttpClient transport, Java 8 the HttpURLConnection one.
        attributes("Main-Class" to mainClassName, "Multi-Release" to "true")
    }
    // A fat jar is not the core module: drop its descriptors so the jar
    // can't masquerade as io.github.josemodi97.sageactive4j.core.
    exclude("module-info.class", "META-INF/versions/*/module-info.class")
    mergeServiceFiles()
}

tasks.jar {
    manifest {
        attributes(
            "Automatic-Module-Name" to "io.github.josemodi97.sageactive4j.cli",
            "Main-Class" to mainClassName
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.name
            artifact(tasks.shadowJar)
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))

            pom {
                name.set(project.name)
                description.set(project.description ?: project.name)
                url.set("https://github.com/JoseModi97/sageactive4j")
                inceptionYear.set("2026")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("josemodi97")
                        name.set("Jose Modi")
                        url.set("https://github.com/JoseModi97")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/JoseModi97/sageactive4j.git")
                    developerConnection.set("scm:git:ssh://git@github.com/JoseModi97/sageactive4j.git")
                    url.set("https://github.com/JoseModi97/sageactive4j")
                }
            }
        }
    }

    repositories {
        maven {
            name = "buildDir"
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

signing {
    setRequired({ gradle.taskGraph.hasTask("publish") && project.hasProperty("signing.keyId") })
    sign(publishing.publications["mavenJava"])
}
