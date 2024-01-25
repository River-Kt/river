kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                // core is inherited
                val modules = modules { project(it) }

                implementation(modules.http)
                implementation(modules.json)

                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.content.renegotiation)
                implementation(libs.ktor.jackson)
            }
        }
    }
}
