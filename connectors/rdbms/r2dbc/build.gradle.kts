kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                api(libs.coroutines.reactive)
                api(libs.r2dbc.spi)
            }
        }

        jvmTest {
            dependencies {
                api(libs.r2dbc.h2)
            }
        }
    }
}
