import Dependencies.Http
import Dependencies.Json
import Dependencies.RiverCore
import Dependencies.File

plugins {
    kotlin("jvm")
}

dependencies {
    api(project.modules.http)
    api(project.modules.json)
    api(project.modules.file)

    api(libs.jackson)

    testImplementation(libs.kotest.wiremock)
    testImplementation(libs.kotlin.wiremock)
}
