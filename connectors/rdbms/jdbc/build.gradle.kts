kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                api(libs.kotlin.reflect)
                api(libs.postgresql.jdbc)
            }
        }
    }
}
