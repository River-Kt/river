import org.gradle.kotlin.dsl.DependencyHandlerScope

object Dependencies {
    val DependencyHandlerScope.RiverCore
        get() = project(mapOf("path" to ":core"))

    val DependencyHandlerScope.AwsCommon
        get() = project(mapOf("path" to ":connectors:aws:common"))

    val DependencyHandlerScope.Http
        get() = project(mapOf("path" to ":utils:http"))

    val Kotlin =
        listOf("kotlin-stdlib-jdk8")
            .map { "org.jetbrains.kotlin:$it" }

    val Slf4j = "org.slf4j:slf4j-api:${Version.Slf4j}"

    val Coroutines =
        listOf(
            "core",
            "reactive",
            "jdk8",
            "jdk9"
        ).map {
            "org.jetbrains.kotlinx:kotlinx-coroutines-$it:${Version.Coroutine}"
        }

    object Aws {
        val Sqs = "software.amazon.awssdk:sqs:${Version.AwsSdk}"
        val Sns = "software.amazon.awssdk:sns:${Version.AwsSdk}"
        val S3 = "software.amazon.awssdk:s3:${Version.AwsSdk}"
        val SesV2 = "software.amazon.awssdk:sesv2:${Version.AwsSdk}"
        val Lambda = "software.amazon.awssdk:lambda:${Version.AwsSdk}"
    }

    val CommonsNet = "commons-net:commons-net:${Version.CommonsNet}"

    val Twilio = "com.twilio.sdk:twilio:${Version.Twilio}"

    val Jackson = "com.fasterxml.jackson.module:jackson-module-kotlin:${Version.Jackson}"

    val Elasticsearch = "co.elastic.clients:elasticsearch-java:${Version.Elasticsearch}"
}
