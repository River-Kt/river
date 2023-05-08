import Dependencies.Http
import Dependencies.Json
import Dependencies.RiverCore
import Dependencies.File

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(RiverCore)
    implementation(Http)
    implementation(Json)
    implementation(Dependencies.Jackson)

    Dependencies.Common.forEach { implementation(it) }
    Dependencies.CommonTest.forEach { testImplementation(it) }
}
