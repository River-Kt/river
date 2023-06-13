dependencies {
    api(project.modules.http)
    api(project.modules.json)
    api(project.modules.file)

    api(libs.jackson)

    testImplementation(libs.kotest.wiremock)
    testImplementation(libs.kotlin.wiremock)
}
