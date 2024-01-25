kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                val modules = modules { project(it) }

                api(modules.http)
                api(modules.json)
                api(libs.jackson)
            }
        }
    }
}
