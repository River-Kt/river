import Dependencies.Common
import Dependencies.CommonTest
import Dependencies.RiverCore

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

repositories {
    mavenCentral()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(RiverCore)
    Common.forEach { implementation(it) }
    implementation(Dependencies.Azure.QueueStorage)
    CommonTest.forEach { testImplementation(it) }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
        jvmTarget = "17"
    }
}
