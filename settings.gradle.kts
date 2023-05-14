pluginManagement {
    plugins {
        kotlin("jvm") version "1.8.20"
        id("org.jetbrains.dokka") version ("1.8.10")
        id("maven-publish")
        id("signing")
        id("io.github.gradle-nexus.publish-plugin") version ("1.3.0")
    }
}

rootProject.name = "river"

include(
    "core",
    "connectors",
    "connectors:amqp",
    "connectors:apache:kafka",
    "connectors:aws:dynamodb",
    "connectors:aws:kinesis",
    "connectors:aws:lambda",
    "connectors:aws:s3",
    "connectors:aws:java-11-http-spi",
    "connectors:aws:sns",
    "connectors:aws:sqs",
    "connectors:aws:ses",
    "connectors:azure:queue-storage",
    "connectors:console",
    "connectors:elasticsearch",
    "connectors:file",
    "connectors:format:csv",
    "connectors:format:json",
    "connectors:ftp",
    "connectors:github",
    "connectors:google:drive",
    "connectors:http",
    "connectors:jms",
    "connectors:mongodb",
    "connectors:mqtt",
    "connectors:rdbms:jdbc",
    "connectors:rdbms:r2dbc",
    "connectors:red-hat:debezium",
    "connectors:twilio",
    "connectors:openai",

    "utils:pool",

    "examples:sqs-to-http",
    "examples:s3-csv-jdbc",
    "examples:ktor-server-json-streaming"
)

fun replaceConnectorsToSingular(project: ProjectDescriptor) {
    project.name =
        project
            .path
            .replaceFirst(":", "")
            .replace(":", "-")
            .replace("connectors", "connector")

    project.children.forEach {
        replaceConnectorsToSingular(it)
    }
}

replaceConnectorsToSingular(project(":connectors"))
