import Dependencies.Common
import Dependencies.CommonTest
import Dependencies.Debezium.Api
import Dependencies.Debezium.Embedded
import Dependencies.Debezium.MySQL
import Dependencies.Jdbc
import Dependencies.RiverCore

plugins {
    kotlin("jvm") version "1.7.20"

    `java-library`
}

repositories {
    mavenCentral()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(RiverCore)
    (Common + Api + Embedded).forEach { implementation(it) }

    testImplementation(Jdbc)
    (CommonTest + MySQL).forEach { testImplementation(it) }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
        jvmTarget = "17"
    }
}
