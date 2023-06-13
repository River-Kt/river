dependencies {
    implementation(libs.debezium.api)
    implementation(libs.debezium.embedded)

    testImplementation(libs.debezium.mysql)
    testImplementation(libs.mysql.jdbc)

    testImplementation(project.modules.jdbc)
}
