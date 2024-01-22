kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                // core is inherited
                val modules = modules { project(it) }

                implementation(modules.http)
                implementation(modules.sqs)

                implementation(libs.wiremock)
                implementation(libs.kotlin.wiremock)
            }
        }
    }
}
