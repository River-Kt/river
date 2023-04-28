import org.jreleaser.model.Active

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin") apply false
    `java-library`
    id("org.jreleaser")
}

repositories {
    mavenCentral()
}

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory.set(file("docs"))
    moduleName.set(project.name)
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "java-library")
    apply(plugin = "org.jreleaser")

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

    val fullname =
        project
            .path
            .replaceFirst(":", "")
            .split(":").joinToString(".")

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
                val releasesRepoUrl = uri(layout.buildDirectory.dir("repos/releases"))
                val snapshotsRepoUrl = uri(layout.buildDirectory.dir("repos/snapshots"))
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            }
        }

        publications {
            create<MavenPublication>("maven") {
                groupId = "com.river"
                artifactId = fullname
                version = "${project.version}"

                afterEvaluate {
                    from(components["kotlin"])
                }

                versionMapping {
                    usage("java-api") {
                        fromResolutionOf("runtimeClasspath")
                    }
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }

                pom {
                    name.set("River-Kt")
                    description.set("Extensions & Enterprise Integrations for Kotlin flows.")
                    url.set("https://river-kt.com")

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

    jreleaser {
        signing {
            active.set(Active.ALWAYS)
            armored.set(true)
        }

        deploy {
            maven {
                nexus2 {
                    create("maven-central") {
                        active.set(Active.ALWAYS)
                        url.set("https://s01.oss.sonatype.org/service/local")
                        snapshotUrl.set("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                        closeRepository.set(false)
                        releaseRepository.set(true)
                    }
                }
            }
        }
    }
}
