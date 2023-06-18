dependencies {
    api(libs.aws.lambda) {
        exclude("software.amazon.awssdk", "netty-nio-client")
    }
}
