import Dependencies.Common
import Dependencies.CommonTest
import Dependencies.KotlinReflect
import Dependencies.Pool
import Dependencies.PostgreSQLJDBC
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
    implementation(KotlinReflect)
    implementation(RiverCore)
    implementation(Pool)
    implementation(PostgreSQLJDBC)
    Common.forEach { implementation(it) }
    CommonTest.forEach { testImplementation(it) }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
        jvmTarget = "17"
    }
}
