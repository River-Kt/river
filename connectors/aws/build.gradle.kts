subprojects {
    kotlin {
        jvm {
            configurations.all {
                exclude("software.amazon.awssdk", "netty-nio-client")
            }
        }

        sourceSets {
            val jvmMain by getting {
                dependencies {
                    val modules = modules { project(it) }

                    api(modules.http)
                    api(rootProject.libs.aws.http.client.spi)
                    api(rootProject.libs.coroutines.reactive)

                    if (project.name != "connector-aws-java-11-http-spi") {
                        api(modules.awsHttp11Spi)
                    }
                }
            }
        }
    }
}
