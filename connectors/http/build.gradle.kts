kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                api(libs.coroutines.jdk9)
            }
        }
    }
}
