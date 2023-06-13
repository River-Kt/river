dependencies {
    // core is inherited

    implementation(project.modules.csv)
    implementation(project.modules.s3)
    implementation(project.modules.jdbc)
    implementation(libs.postgresql.jdbc)
}
