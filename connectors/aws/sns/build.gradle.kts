dependencies {
    api(libs.aws.sns) {
        exclude("software.amazon.awssdk", "netty-nio-client")
    }
}
