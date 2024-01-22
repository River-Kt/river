kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                val modules = modules { project(it) }

                api(modules.http)
                api(modules.json)
                api(modules.file)

                api(libs.jackson)
            }
        }

        jvmTest {
            dependencies {
                api(libs.kotlin.wiremock)
                api(libs.kotest.wiremock)
            }
        }
    }
}
