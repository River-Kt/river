import Dependencies.Aws.HttpClientSpi
import Dependencies.Http
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
    implementation(Http)
    implementation(HttpClientSpi)
    Dependencies.Common.forEach { implementation(it) }
    Dependencies.CommonTest.forEach { testImplementation(it) }
}
