dependencies {
    implementation(project.modules.pool)

    implementation(libs.jms.api)

    testImplementation(libs.artemis.client)
    testImplementation(libs.artemis.server)
}
