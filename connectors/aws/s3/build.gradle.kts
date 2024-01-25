kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(rootProject.libs.aws.s3)
            }
        }
    }
}
