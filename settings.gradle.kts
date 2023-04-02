pluginManagement {
    plugins {
        kotlin("jvm") version "1.7.22"
        id("org.jetbrains.dokka") version ("1.7.20")
        id("maven-publish")
        id("signing")
        id("io.github.gradle-nexus.publish-plugin") version ("1.3.0")
    }
}

rootProject.name = "river"

include(
    "core",
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
    "connectors:jms",
    "connectors:mongodb",
    "connectors:mqtt",
    "connectors:rdbms:jdbc",
    "connectors:rdbms:r2dbc",
    "connectors:red-hat:debezium",
    "connectors:twilio",
    "utils:http",
    "utils:pool"
)
