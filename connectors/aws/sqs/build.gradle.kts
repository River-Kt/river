dependencies {
    api(libs.aws.sqs) {
        exclude("software.amazon.awssdk", "netty-nio-client")
    }
}
