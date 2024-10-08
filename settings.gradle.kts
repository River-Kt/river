dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "river"

include(
    "core",
//    "connectors",
    "connectors:aws",
    "connectors:amqp",
    "connectors:apache:kafka",
    "connectors:aws:dynamodb",
    "connectors:aws:kinesis",
    "connectors:aws:lambda",
    "connectors:aws:s3",
    "connectors:aws:sns",
    "connectors:aws:sqs",
    "connectors:aws:ses",
    "connectors:azure:queue-storage",
    "connectors:console",
    "connectors:elasticsearch",
    "connectors:file",
    "connectors:format:csv",
    "connectors:format:json",
    "connectors:format:positional-flat-line",
    "connectors:ftp",
    "connectors:github",
    "connectors:google:common",
    "connectors:google:drive",
    "connectors:http",
    "connectors:jms",
    "connectors:ktor",
    "connectors:ktor:network",
    "connectors:mongodb",
    "connectors:mqtt",
    "connectors:rdbms:jdbc",
    "connectors:rdbms:r2dbc",
    "connectors:red-hat:debezium",
    "connectors:openai",
    "connectors:redis",
    "examples:sqs-to-http",
    "examples:s3-csv-jdbc",
    "examples:debezium-csv-s3",
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
