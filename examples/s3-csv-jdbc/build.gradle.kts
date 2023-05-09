import Dependencies.Aws
import Dependencies.Coroutines
import Dependencies.CsvConnector
import Dependencies.Http
import Dependencies.Jdbc
import Dependencies.KotlinWiremock
import Dependencies.PostgreSQLJDBC
import Dependencies.RiverCore
import Dependencies.S3Connector
import Dependencies.SqsConnector
import Dependencies.Wiremock

plugins {
    kotlin("jvm")
}

dependencies {
    Coroutines.forEach { implementation(it) }
    implementation(RiverCore)
    implementation(CsvConnector)
    implementation(S3Connector)
    implementation(Jdbc)
    implementation(PostgreSQLJDBC)
    implementation(Aws.S3) {
        exclude("software.amazon.awssdk", "netty-nio-client")
    }
}
