import Dependencies.Http
import Dependencies.Json
import Dependencies.RiverCore

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(RiverCore)
    implementation(Http)
    implementation(Json)
    implementation(Dependencies.Jackson)
    implementation(Dependencies.JavaJwt)

    Dependencies.Common.forEach { implementation(it) }
    Dependencies.CommonTest.forEach { testImplementation(it) }

    testImplementation(Dependencies.KotlinWiremock)
    testImplementation(Dependencies.KotestWiremock)
}
