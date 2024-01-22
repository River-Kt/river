kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(libs.reactor.kafka)
                api(libs.coroutines.reactive)
            }
        }
    }
}
