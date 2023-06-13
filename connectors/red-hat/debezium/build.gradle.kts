dependencies {
    api(libs.debezium.api)
    api(libs.debezium.embedded)

    testImplementation(libs.debezium.mysql)
    testImplementation(libs.mysql.jdbc)

    testImplementation(project.modules.jdbc)
}
