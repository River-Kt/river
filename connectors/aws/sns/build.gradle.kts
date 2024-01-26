kotlin {
    sourceSets {
        macosMain {

        }

        jvmMain {
            dependencies {
                api(libs.aws.sns)
            }
        }
    }
}
