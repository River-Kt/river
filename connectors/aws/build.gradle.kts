subprojects {
    configurations.all {
        resolutionStrategy {
            exclude("software.amazon.awssdk", "netty-nio-client")
        }
    }

    dependencies {
        implementation(project(":connector:connector-http"))

        implementation(rootProject.libs.aws.http.client.spi)
        implementation(rootProject.libs.coroutines.reactive)

        if (project.name != "connector-aws-java-11-http-spi") {
            implementation(rootProject.modules.awsHttp11Spi)
        }
    }
}
