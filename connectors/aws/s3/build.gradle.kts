dependencies {
    api(libs.aws.s3) {
        exclude("software.amazon.awssdk", "netty-nio-client")
    }
}
