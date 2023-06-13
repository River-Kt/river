dependencies {
    // core is inherited

    implementation(project.modules.http)
    implementation(project.modules.sqs)

    implementation(libs.wiremock)
    implementation(libs.kotlin.wiremock)
}
