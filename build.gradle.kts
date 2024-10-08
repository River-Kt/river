@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.kotlin.cli.common.toBooleanLenient
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
    alias(libs.plugins.test.logger) apply false

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
    apply(plugin = "com.adarshr.test-logger")

    version = rootProject.libs.versions.river.get()
    group = "com.river-kt"

    repositories {
        mavenCentral()
        google()
    }

    configure<TestLoggerExtension> {
        theme = ThemeType.MOCHA
        showExceptions = true
        showStackTraces = true
        showFullStackTraces = false
        showCauses = true
        slowThreshold = 10000
        showSummary = true
        showSimpleNames = false
        showPassed = true
        showSkipped = true
        showFailed = true
        showStandardStreams = false
        showPassedStandardStreams = true
        showSkippedStandardStreams = true
        showFailedStandardStreams = true
        logLevel = LogLevel.LIFECYCLE
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()

        onlyIf { !skipTests() }
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
            freeCompilerArgs = listOf(
                "-Xcontext-receivers",
                "-Xexpect-actual-classes"
            )
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
                resources.setSrcDirs(resources.srcDirs + file("$rootDir/src/jvmTest/resources"))

                dependencies {
                    api(rootProject.libs.kotest.junit5)
                    api(rootProject.libs.logback)
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

    onAndroid {
        namespace = "com.river"
        compileSdk = rootProject.libs.versions.android.compile.sdk.get().toInt()

        publishing {
            multipleVariants {
                withJavadocJar()
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
                        username = System.getenv("OSSRH_USER_TOKEN_USERNAME")
                        password = System.getenv("OSSRH_USER_TOKEN_PASSWORD")
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
    afterEvaluate { if (pluginManager.hasPlugin("com.android.library")) block() }
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
