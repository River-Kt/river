dependencies {
    api(project.modules.pool)

    api(libs.jms.api)

    testImplementation(libs.artemis.client)
    testImplementation(libs.artemis.server)
}
