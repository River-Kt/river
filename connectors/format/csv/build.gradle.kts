kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                val modules = modules { project(it) }
                api(modules.file)
            }
        }
    }
}
