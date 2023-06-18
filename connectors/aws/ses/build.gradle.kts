dependencies {
    api(libs.aws.ses) {
        exclude("software.amazon.awssdk", "netty-nio-client")
    }
}
