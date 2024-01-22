kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(libs.amqp)
            }
        }
    }
}
