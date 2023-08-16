import org.jetbrains.dokka.gradle.AbstractDokkaTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.nexus.publish) apply false

    `maven-publish`
    signing
    `java-library`
}

repositories {
    mavenCentral()
}

tasks.dokkaHtmlMultiModule.configure {
    dependsOn(":connector:dokkaHtmlMultiModule")
    outputDirectory.set(file("docs"))
    moduleName.set(project.name)
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "java-library")
    apply(plugin = "signing")

    version = "1.0.0-alpha10"

    group = "com.river-kt"

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
        skipExamples()

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

                artifact(tasks["jar"])

                artifact(tasks["sourcesJar"]) {
                    classifier = "sources"
                }

                artifact(tasks["javadocJar"]) {
                    classifier = "javadoc"
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

                    withXml {
                        asNode().appendNode("dependencies").apply {
                            for (dependency in configurations["api"].dependencies) {
                                appendNode("dependency").apply {
                                    appendNode("groupId", dependency.group)
                                    appendNode("artifactId", dependency.name)
                                    appendNode("version", dependency.version)

                                    val excludeRules =
                                        if (dependency is ModuleDependency) dependency.excludeRules
                                        else emptySet()

                                    if (excludeRules.isNotEmpty()) {
                                        appendNode("exclusions").apply {
                                            appendNode("exclusion").apply {
                                                excludeRules.forEach { excludeRule ->
                                                    appendNode("groupId", excludeRule.group)
                                                    appendNode("artifactId", excludeRule.module)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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

    signing {
        useInMemoryPgpKeys(signingKeyId, signingSecretKey, signingPassword)

        sign(publishing.publications["maven"])
        sign(tasks["javadocJar"])
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

    tasks.javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        }
    }

    dependencies {
        api(rootProject.libs.coroutines)
        testImplementation(rootProject.libs.kotest.junit5)
    }
}

fun Task.skipExamples() {
    onlyIf { !project.path.contains("examples") }
}
