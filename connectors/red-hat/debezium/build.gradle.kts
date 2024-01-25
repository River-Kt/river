kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                api(libs.debezium.api)
                api(libs.debezium.embedded)
            }
        }

        jvmTest {
            dependencies {
                val modules = modules { project(it) }

                api(libs.debezium.mysql)
                api(libs.mysql.jdbc)
                api(modules.jdbc)
            }
        }
    }
}
