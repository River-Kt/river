dependencies {
    // core is inherited

    implementation(project.modules.debezium)
    implementation(project.modules.csv)
    implementation(project.modules.json)
    implementation(project.modules.s3)

    implementation(libs.debezium.postgres)
    implementation(libs.postgresql.jdbc)
}
