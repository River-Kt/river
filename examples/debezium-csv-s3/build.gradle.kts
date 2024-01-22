kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                // core is inherited
                val modules = modules { project(it) }

                implementation(modules.debezium)
                implementation(modules.csv)
                implementation(modules.json)
                implementation(modules.s3)

                implementation(libs.debezium.postgres)
                implementation(libs.postgresql.jdbc)
            }
        }
    }
}
