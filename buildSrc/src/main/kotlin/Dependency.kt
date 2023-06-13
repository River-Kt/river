import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.project

object Dependencies {
    val DependencyHandlerScope.RiverCore
        get() = project(mapOf("path" to ":core"))

    val DependencyHandlerScope.Http
        get() = project(mapOf("path" to ":connector:connector-http"))

    val DependencyHandlerScope.Json
        get() = project(mapOf("path" to connector("format-json")))

    val DependencyHandlerScope.Pool
        get() = project(mapOf("path" to ":utils:pool"))

    val DependencyHandlerScope.Jdbc
        get() = project(mapOf("path" to connector("rdbms-jdbc")))

    val DependencyHandlerScope.File
        get() = project(mapOf("path" to ":connector:connector-file"))

    val DependencyHandlerScope.SqsConnector
        get() = project(mapOf("path" to ":connector:connector-aws:connector-aws-sqs"))

    val DependencyHandlerScope.S3Connector
        get() = project(mapOf("path" to ":connector:connector-aws:connector-aws-s3"))

    val DependencyHandlerScope.CsvConnector
        get() = project(mapOf("path" to ":connector:connector-format:connector-format-csv"))

    fun connector(name: String) =
        ":connector:connector-${name.split("-").first()}:connector-$name"

    val CoroutinesCore =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.Coroutine}"

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

    val DependencyHandlerScope.ConnectorCommon
        get() = listOf(RiverCore) + Common

    object Aws {
        val HttpClientSpi = "software.amazon.awssdk:http-client-spi:${Version.AwsSdk}"
        val Sqs = "software.amazon.awssdk:sqs:${Version.AwsSdk}"
        val Sns = "software.amazon.awssdk:sns:${Version.AwsSdk}"
        val S3 = "software.amazon.awssdk:s3:${Version.AwsSdk}"
        val Ses = "software.amazon.awssdk:ses:${Version.AwsSdk}"
        val Lambda = "software.amazon.awssdk:lambda:${Version.AwsSdk}"
    }

    val CommonsNet = "commons-net:commons-net:${Version.CommonsNet}"

    val ApacheFtpServer = "org.apache.ftpserver:ftpserver-core:${Version.ApacheFtpServer}"

    val MockFtpServer = "org.mockftpserver:MockFtpServer:${Version.MockFtpServer}"

    val Twilio = "com.twilio.sdk:twilio:${Version.Twilio}"

    val Jackson = "com.fasterxml.jackson.module:jackson-module-kotlin:${Version.Jackson}"

    val Elasticsearch = "co.elastic.clients:elasticsearch-java:${Version.Elasticsearch}"

    val Amqp = "com.rabbitmq:amqp-client:${Version.RabbitMQ}"

    object Jms {
        val Api = "javax.jms:javax.jms-api:${Version.Jms}"
        val ArtemisClient = "org.apache.activemq:artemis-jms-client:${Version.ActiveMQArtemis}"
        val ArtemisServer = "org.apache.activemq:artemis-server:${Version.ActiveMQArtemis}"
    }

    object R2dbc {
        val spi = "io.r2dbc:r2dbc-spi:${Version.R2dbc}"
        val h2 = "io.r2dbc:r2dbc-h2:${Version.R2dbc}"
    }

    object Azure {
        val QueueStorage = "com.azure:azure-storage-queue:${Version.Azure}"
    }

    object Kotest {
        val JUnit5 = "io.kotest:kotest-runner-junit5:${Version.Kotest}"
    }

    val PostgreSQLJDBC = "org.postgresql:postgresql:${Version.PostgreSQLJDBC}"
    val MySQLJDBC = "com.mysql:mysql-connector-j:${Version.MySQLJDBC}"

    object Debezium {
        val Api = "io.debezium:debezium-api:${Version.Debezium}"
        val Embedded = "io.debezium:debezium-embedded:${Version.Debezium}"

        val MySQL = "io.debezium:debezium-connector-mysql:${Version.Debezium}"
    }

    val MongoDB = "org.mongodb:mongodb-driver-reactivestreams:${Version.MongoDB}"

    val CommonTest = listOf(Kotest.JUnit5)

    val Wiremock = "com.github.tomakehurst:wiremock:${Version.Wiremock}"
    val KotlinWiremock = "com.marcinziolo:kotlin-wiremock:${Version.KotlinWiremock}"
    val KotestWiremock = "io.kotest.extensions:kotest-extensions-wiremock:${Version.KotestWiremock}"

    val JavaJwt = "com.auth0:java-jwt:${Version.JavaJwt}"

    object Ktor {
        object Server {
            val Core = "io.ktor:ktor-server-core-jvm:${Version.Ktor}"
            val Netty = "io.ktor:ktor-server-netty-jvm:${Version.Ktor}"
            val ContentRenegotiation = "io.ktor:ktor-server-content-negotiation:${Version.Ktor}"
        }

        val Jackson = "io.ktor:ktor-serialization-jackson:${Version.Ktor}"
    }
}

object Modules {
    val core =
        Module(":core")

    val awsHttp11Spi =
        Module(":connector:connector-aws:connector-aws-java-11-http-spi")

    val http =
        Module(":connector:connector-http")

    val json =
        Module(":connector:connector-format:connector-format-json")

    val pool =
        Module(":utils:pool")

    val jdbc =
        Module(":connector:connector-rdbms:connector-rdbms-jdbc")

    val file =
        Module(":connector:connector-file")

    val DependencyHandlerScope.File
        get() = project(mapOf("path" to ":connector:connector-file"))

    val DependencyHandlerScope.SqsConnector
        get() = project(mapOf("path" to ":connector:connector-aws:connector-aws-sqs"))

    val DependencyHandlerScope.S3Connector
        get() = project(mapOf("path" to ":connector:connector-aws:connector-aws-s3"))

    val DependencyHandlerScope.CsvConnector
        get() = project(mapOf("path" to ":connector:connector-format:connector-format-csv"))
}

class Module(val name: String)

val Project.modules
    get() = Modules

fun DependencyHandler.api(module: Module): Dependency? =
    add("api", this.project(module.name))

fun DependencyHandler.implementation(module: Module): Dependency? =
    add("implementation", this.project(module.name))

fun DependencyHandler.compileOnly(module: Module): Dependency? =
    add("compileOnly", this.project(module.name))

fun DependencyHandler.testImplementation(module: Module): Dependency? =
    add("testImplementation", this.project(module.name))
