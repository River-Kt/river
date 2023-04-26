import org.jetbrains.dokka.gradle.DokkaMultiModuleTask

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("signing")
    id("io.github.gradle-nexus.publish-plugin") apply false
    `java-library`
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
        publications {
            create<MavenPublication>("maven") {
                groupId = "com.river"
                version = "0.0.1"
                artifactId = fullname

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
}
