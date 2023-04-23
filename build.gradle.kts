plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("signing")
    id("io.github.gradle-nexus.publish-plugin") apply false
}

repositories {
    mavenCentral()
}

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory.set(project.file("docs"))
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")

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

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "com.river"
                version = "0.0.1"

                afterEvaluate {
                    from(components["kotlin"])
                    artifactId = tasks.jar.get().archiveBaseName.get()
                }
            }
        }
    }
}
