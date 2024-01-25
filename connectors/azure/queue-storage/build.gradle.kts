kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                api(libs.azure.queue.storage)
            }
        }
    }
}
