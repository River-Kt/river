import Dependencies.Http
import Dependencies.Json
import Dependencies.RiverCore
import Dependencies.File

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project.modules.http)
    implementation(project.modules.json)
    implementation(project.modules.file)

    implementation(libs.jackson)

    testImplementation(libs.kotest.wiremock)
    testImplementation(libs.kotlin.wiremock)
}
