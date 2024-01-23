@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.android.build.gradle.LibraryExtension as AndroidExtension

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

    version = "1.0.0-alpha13"
    group = "com.river-kt"

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
            val commonMain by getting {
                dependencies {
                    api(rootProject.libs.coroutines)
                    api(rootProject.libs.kotlinx.datetime)
                }
            }

            val commonTest by getting {
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

            val jvmTest by getting {
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
        extensions.configure<AndroidExtension> {
            namespace = "com.river"
            compileSdk = 30
        }
    }

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

        publications.forEach { publication ->
            if (publication !is MavenPublication) {
                return@forEach
            }

            publication.artifact(tasks["javadocJar"]) {
                classifier = "javadoc"
            }

            publication.pom {
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

//                withXml {
//                    asNode().appendNode("dependencies").apply {
//                        val dependencies = configurations.asMap["api"]?.dependencies ?: emptySet()
//
//                        for (dependency in dependencies) {
//                            appendNode("dependency").apply {
//                                appendNode("groupId", dependency.group)
//                                appendNode("artifactId", dependency.name)
//                                appendNode("version", dependency.version)
//
//                                val excludeRules =
//                                    if (dependency is ModuleDependency) dependency.excludeRules
//                                    else emptySet()
//
//                                if (excludeRules.isNotEmpty()) {
//                                    appendNode("exclusions").apply {
//                                        appendNode("exclusion").apply {
//                                            excludeRules.forEach { excludeRule ->
//                                                appendNode("groupId", excludeRule.group)
//                                                appendNode("artifactId", excludeRule.module)
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
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

    onWindows {
        val publishWindowsArtifacts by tasks.registering {
            dependsOn(
                tasks
                    .withType<PublishToMavenRepository>()
                    .filter { it.name.contains("mingw", ignoreCase = true) }
            )
        }
    }

    onMacOS {
        val publishOSXArtifacts by tasks.registering {
            val appleOs = listOf("ios", "macos", "watchos", "tvos")

            dependsOn(
                tasks
                    .withType<PublishToMavenRepository>()
                    .filter { p ->
                        appleOs.any { p.name.contains(it, ignoreCase = true) }
                    }
            )
        }
    }

    onLinux {
        val publishJvmArtifacts by tasks.registering {
            dependsOn(
                tasks
                    .withType<PublishToMavenRepository>()
                    .filter { it.name.contains("jvm", ignoreCase = true) }
            )
        }

        val publishLinuxArtifacts by tasks.registering {
            dependsOn(
                tasks
                    .withType<PublishToMavenRepository>()
                    .filter { it.name.contains("linux", ignoreCase = true) }
            )
        }

        val publishJsArtifacts by tasks.registering {
            dependsOn(
                tasks
                    .withType<PublishToMavenRepository>()
                    .filter { it.name.contains("js", ignoreCase = true) }
            )
        }
    }

    signing {
        useInMemoryPgpKeys(signingKeyId, signingSecretKey, signingPassword)
        sign(publishing.publications)
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        skipExamples()
        dependsOn(tasks.withType<Sign>())
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

//    tasks.javadoc {
//        if (JavaVersion.current().isJava9Compatible) {
//            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
//        }
//    }
}

fun Task.skipExamples() {
    onlyIf { !project.path.contains("examples") }
}

fun Project.onAndroidEnabled(block: () -> Unit) {
    afterEvaluate {
        if (pluginManager.hasPlugin("com.android.library")) block()
    }
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
