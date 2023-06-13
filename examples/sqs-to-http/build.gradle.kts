import Dependencies.Aws
import Dependencies.Coroutines
import Dependencies.Http
import Dependencies.KotlinWiremock
import Dependencies.RiverCore
import Dependencies.SqsConnector
import Dependencies.Wiremock

plugins {
    kotlin("jvm")
}

dependencies {
    Coroutines.forEach { implementation(it) }
    implementation(RiverCore)
    implementation(SqsConnector)
    implementation(project.modules.http)
    implementation(KotlinWiremock)
    implementation(Wiremock)
    implementation(Aws.Sqs) {
        exclude("software.amazon.awssdk", "netty-nio-client")
    }
}
