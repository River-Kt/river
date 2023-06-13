dependencies {
    // core is inherited

    implementation(project.modules.http)
    implementation(project.modules.json)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.content.renegotiation)
    implementation(libs.ktor.jackson)
}
