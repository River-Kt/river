kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                api(libs.elasticsearch)
                api(libs.jackson)
            }
        }
    }
}
