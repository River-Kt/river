@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.android.build.gradle.LibraryExtension
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.setup.android.sdk)
    alias(libs.plugins.android) apply false
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.nexus.publish) apply false
    alias(libs.plugins.os.detector)

    `maven-publish`
    signing
}

group = "com.river-kt"

repositories {
    mavenCentral()
    google()
}

tasks.dokkaHtmlMultiModule.configure {
    dependsOn(":connector:dokkaHtmlMultiModule")
    outputDirectory.set(file("docs"))
    moduleName.set(project.name)
}

kotlin {
    jvm {
        withSourcesJar(false)
    }
}

setupAndroidSdk {
    sdkToolsVersion("11076708_latest")
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.multiplatform")
    apply(plugin = "io.kotest.multiplatform")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "signing")
    apply(plugin = "com.google.osdetector")

    version = rootProject.libs.versions.river.get()
    group = "com.river-kt"

    repositories {
        mavenCentral()
        google()
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

    kotlin {
        jvmToolchain(17)

        compilerOptions {
            freeCompilerArgs = listOf("-Xcontext-receivers")
        }

        jvm().compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
            }
        }

        sourceSets {
            commonMain {
                dependencies {
                    api(rootProject.libs.coroutines)
                    api(rootProject.libs.kotlinx.datetime)
                }
            }

            commonTest {
                dependencies {
                    api(rootProject.libs.kotest.assertions.core)
                    api(rootProject.libs.kotest.engine)
                    api(rootProject.libs.turbine)
                    api(rootProject.libs.kotlin.reflect)
                }

                languageSettings {
                    optIn("com.river.core.ExperimentalRiverApi")
                    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                }
            }

            jvmTest {
                dependencies {
                    api(rootProject.libs.kotest.junit5)
                }
            }

            onAndroidEnabled {
                val androidUnitTest by getting {
                    dependencies {
                        implementation(rootProject.libs.kotest.junit5)
                    }
                }
            }
        }
    }

    pluginManager.withPlugin("com.android.library") {
        extensions.configure<LibraryExtension> {
            namespace = "com.river"
            compileSdk = rootProject.libs.versions.android.compile.sdk.get().toInt()

            publishing {
                multipleVariants {
                    withJavadocJar()
                }
            }
        }
    }

    afterEvaluate {
        tasks.dokkaHtml.configure {
            skipExamples()

            dokkaSourceSets {
                configureEach {
                    samples.from("src/sample/kotlin")
                }
            }
        }

        val javadocJar by tasks.registering(Jar::class) {
            archiveClassifier.set("javadoc")
            from(tasks.dokkaHtml)
            dependsOn("dokkaHtml")
            skipExamples()
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

            publications.withType<MavenPublication> {
                artifact(tasks["javadocJar"]) { classifier = "javadoc" }

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
                            name.set("The MIT License")
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

        val signingKeyId by lazy { System.getenv("SIGNING_KEY_ID") }
        val signingPassword by lazy { System.getenv("SIGNING_PASSWORD") }
        val signingSecretKey by lazy { System.getenv("SIGNING_SECRET_FILE") }

        tasks.withType<Sign>().configureEach {
            skipExamples()

            onlyIf {
                signingKeyId != null && signingPassword != null && signingSecretKey != null
            }
        }

        val signAllPublications by tasks.registering {
            dependsOn(tasks.withType<Sign>())
        }

        signing {
            useInMemoryPgpKeys(signingKeyId, signingSecretKey, signingPassword)
            sign(publishing.publications)
        }

        tasks.withType<AbstractPublishToMaven>().configureEach {
            skipExamples()
            dependsOn(signAllPublications)
        }

        tasks.withType<AbstractDokkaTask>().configureEach {
            val task = this.path.split(":").last()

            skipExamples()

            dependsOn(
                subprojects.mapNotNull {
                    runCatching { tasks.getByPath("${it.path}:$task") }.getOrNull()
                }
            )
        }
    }
}

fun Task.skipExamples() {
    onlyIf { !project.path.contains("examples") }
}

fun Project.onAndroidEnabled(block: () -> Unit) {
    if (pluginManager.hasPlugin("com.android.library")) block()
}

fun Project.onWindows(block: () -> Unit) {
    if (os == "windows") block()
}

fun Project.onLinux(block: () -> Unit) {
    if (os == "linux") block()
}

fun Project.onMacOS(block: () -> Unit) {
    if (os == "osx") block()
}

val Project.os: String
    get() = osdetector.os
