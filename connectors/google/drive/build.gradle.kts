dependencies {
    implementation(project.modules.http)
    implementation(project.modules.json)
    implementation(libs.jackson)
    implementation(libs.java.jwt)

    testImplementation(libs.kotest.wiremock)
}
