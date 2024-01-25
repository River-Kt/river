kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                // core is inherited
                val modules = modules { project(it) }

                implementation(modules.csv)
                implementation(modules.s3)
                implementation(modules.jdbc)

                implementation(libs.postgresql.jdbc)
            }
        }
    }
}
