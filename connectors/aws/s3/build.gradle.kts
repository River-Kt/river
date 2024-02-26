kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(rootProject.libs.aws.s3)
            }
        }
    }
}
