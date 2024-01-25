kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                api(libs.coroutines.reactive)
                api(libs.kotlin.reflect)
                api(libs.mongodb)
            }
        }
    }
}
