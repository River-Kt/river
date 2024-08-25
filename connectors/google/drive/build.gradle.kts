plugins {
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                val modules = modules { project(it) }

                api(modules.googleCommon)
            }
        }
    }
}
