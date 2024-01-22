kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                val modules = modules { project(it) }

                api(modules.file)
                api(libs.commons.net)
            }
        }

        val jvmTest by getting {
            dependencies {
                api(libs.mock.ftpserver)
            }
        }
    }
}
