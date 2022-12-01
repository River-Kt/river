import org.gradle.kotlin.dsl.DependencyHandlerScope

object Dependencies {
    val DependencyHandlerScope.RiverCore
        get() = project(mapOf("path" to ":core"))

    val DependencyHandlerScope.AwsCommon
        get() = project(mapOf("path" to ":connectors:aws:common"))

    val DependencyHandlerScope.Http
        get() = project(mapOf("path" to ":utils:http"))

    val DependencyHandlerScope.Pool
        get() = project(mapOf("path" to ":utils:pool"))

    val Coroutines =
        listOf(
            "core",
            "reactive",
            "jdk8",
            "jdk9"
        ).map {
            "org.jetbrains.kotlinx:kotlinx-coroutines-$it:${Version.Coroutine}"
        }

    val Kotlin =
        listOf("kotlin-stdlib-jdk8")
            .map { "org.jetbrains.kotlin:$it" }

    val KotlinReflect = "org.jetbrains.kotlin:kotlin-reflect"

    val Slf4j = "org.slf4j:slf4j-api:${Version.Slf4j}"

    val Common: List<String> = Kotlin + Coroutines + Slf4j

    object Aws {
        val HttpClientSpi = "software.amazon.awssdk:http-client-spi:${Version.AwsSdk}"
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

    val Amqp = "com.rabbitmq:amqp-client:${Version.RabbitMQ}"

    object Jms {
        val Api = "javax.jms:javax.jms-api:${Version.Jms}"
        val ArtemisClient = "org.apache.activemq:artemis-jms-client:${Version.ActiveMQArtemis}"
        val ArtemisServer = "org.apache.activemq:artemis-server:${Version.ActiveMQArtemis}"
    }

    object Azure {
        val QueueStorage = "com.azure:azure-storage-queue:${Version.Azure}"
    }

    object Kotest {
        val JUnit5 = "io.kotest:kotest-runner-junit5:${Version.Kotest}"
    }

    val PostgreSQLJDBC = "org.postgresql:postgresql:${Version.PostgreSQLJDBC}"

    val CommonTest = listOf(Kotest.JUnit5)
}
