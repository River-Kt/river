dependencies {
    api(project.modules.http)
    api(project.modules.json)
    api(libs.jackson)
    api(libs.java.jwt)

    testImplementation(libs.kotest.wiremock)
}
