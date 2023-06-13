import Dependencies.Coroutines
import Dependencies.Http
import Dependencies.Json
import Dependencies.Ktor
import Dependencies.RiverCore

plugins {
    kotlin("jvm")
}

dependencies {
    Coroutines.forEach { implementation(it) }
    implementation(RiverCore)
    implementation(project.modules.http)
    implementation(project.modules.json)

    implementation(Ktor.Server.Core)
    implementation(Ktor.Server.Netty)
    implementation(Ktor.Server.ContentRenegotiation)
    implementation(Ktor.Jackson)
}
