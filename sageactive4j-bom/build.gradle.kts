plugins {
    `java-platform`
    `maven-publish`
    signing
}

description = "Bill of materials for sageactive4j: import this to pin matching versions of every " +
        "sageactive4j module without repeating version numbers."

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":sageactive4j-core"))
        api(project(":sageactive4j-servlet"))
        api(project(":sageactive4j-jakarta"))
        api(project(":sageactive4j-spring-boot2-starter"))
        api(project(":sageactive4j-spring-boot3-starter"))
        api(project(":sageactive4j-cli"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "sageactive4j-bom"
            from(components["javaPlatform"])

            pom {
                name.set("sageactive4j-bom")
                description.set(project.description)
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
