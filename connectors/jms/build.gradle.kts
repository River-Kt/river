kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(libs.jms.api)
            }
        }

        val jvmTest by getting {
            dependencies {
                api(libs.artemis.client)
                api(libs.artemis.server)
            }
        }
    }
}
