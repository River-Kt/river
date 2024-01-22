kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                val modules = modules { project(it) }

                api(modules.http)
                api(modules.json)
                api(libs.jackson)
                api(libs.java.jwt)
            }
        }

        jvmTest {
            dependencies {
                api(libs.kotest.wiremock)
            }
        }
    }
}
