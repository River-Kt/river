import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin") apply false
    `java-library`
    signing
}

repositories {
    mavenCentral()
}

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory.set(file("docs"))
    moduleName.set(project.name)
}

version = "0.0.1-alpha01"

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "java-library")
    apply(plugin = "signing")

    version = "0.0.1-alpha01"

    java {
        withJavadocJar()
        withSourcesJar()
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
            jvmTarget = "17"
        }
    }

    tasks.dokkaHtml.configure {
        dokkaSourceSets {
            configureEach {
                samples.from("src/sample/kotlin")
            }
        }
    }

    publishing {
        repositories {
            maven {
                name = "OSSRH"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("RELEASER_NEXUS2_USERNAME")
                    password = System.getenv("RELEASER_NEXUS2_PASSWORD")
                }
            }
        }

        publications {
            create<MavenPublication>("maven") {
                groupId = "com.river-kt"
                artifactId = project.name
                version = "${project.version}"

                afterEvaluate {
                    from(components["kotlin"])
                }

                pom {
                    name.set(project.name)
                    description.set("Extensions & Enterprise Integrations for Kotlin flows.")

                    url.set("https://river-kt.com")

                    scm {
                        connection.set("scm:git:git:github.com/River-Kt/river.git")
                        developerConnection.set("scm:git:ssh://github.com/River-Kt/river.git")
                        url.set("https://github.com/River-Kt/river")
                    }

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/license/mit/")
                        }
                    }

                    developers {
                        developer {
                            id.set("gabfssilva")
                            name.set("Gabriel Francisco")
                            email.set("gabfssilva@gmail.com")
                        }
                    }
                }
            }
        }
    }

    signing {
        val keyId = System.getenv("SIGNING_KEY_ID")
        val password = System.getenv("SIGNING_PASSWORD")
        val secretKey = System.getenv("SIGNING_SECRET_FILE")

        useInMemoryPgpKeys(keyId, secretKey, password)

        sign(publishing.publications["maven"])
        sign(tasks["javadocJar"])
    }

    tasks.javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        }
    }
}
