kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.aws.sns)
            }
        }
    }
}
