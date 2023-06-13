subprojects {
    configurations.all {
        resolutionStrategy {
            exclude("software.amazon.awssdk", "netty-nio-client")
        }
    }

    dependencies {
        api(rootProject.modules.http)

        api(rootProject.libs.aws.http.client.spi)
        api(rootProject.libs.coroutines.reactive)

        if (project.name != "connector-aws-java-11-http-spi") {
            api(rootProject.modules.awsHttp11Spi)
        }
    }
}
