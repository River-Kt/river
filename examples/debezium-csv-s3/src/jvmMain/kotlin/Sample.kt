@file:OptIn(FlowPreview::class)

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.smithy.kotlin.runtime.net.url.Url
import com.fasterxml.jackson.databind.node.ObjectNode
import com.river.connector.aws.s3.uploadSplitItems
import com.river.connector.format.csv.rawCsv
import com.river.connector.format.json.asParsedJson
import com.river.connector.red.hat.debezium.debeziumFlow
import com.river.core.*
import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.format.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.time.Duration.Companion.seconds

@ExperimentalRiverApi
suspend fun main() = coroutineScope {
    s3.createBucket { bucket = "catalog" }

    debeziumFlow { DebeziumEngine.create(Json::class.java).using(debeziumProperties) }
        .mapAsync(1000) {
            val name = it.record.destination().replace("orders.public.", "")
            val channel = sinkByDestination(name)
            channel.send(it.record)
            it.markProcessed()
        }
        .withIndex()
        .sample(1.seconds)
        .collect { println("${it.index} elements processed") }
}

val mutex = Mutex()
val csvSinks = mutableMapOf<String, Channel<ChangeEvent<String, String>>>()

context(CoroutineScope)
suspend fun sinkByDestination(name: String) =
    csvSinks[name] ?: mutex.withLock {
        csvSinks[name] ?: csvSink(name).let { (channel, _) ->
            csvSinks[name] = channel
            channel
        }
    }

context(CoroutineScope)
fun csvSink(
    name: String
): Pair<Channel<ChangeEvent<String, String>>, Job> {
    val csvSink = Channel<ChangeEvent<String, String>>()

    val sinkJob =
        s3
            .uploadSplitItems(
                bucket = "catalog",
                upstream = csvSink.consumeAsFlow().asCsv(),
                splitStrategy = GroupStrategy.TimeWindow(1000, 1.seconds),
                key = { "$name.csv.$it" }
            ) { line -> line.toByteArray() }
            .launch()

    return csvSink to sinkJob
}

fun Flow<ChangeEvent<String, String>>.asCsv(): Flow<String> =
    map { it.value() }
        .asParsedJson<ObjectNode>()
        .rawCsv { json ->
            json.fieldNames()
                .asSequence()
                .map { json[it].asText() }
                .toList()
        }
        .intersperse("\n")

val s3 =
    S3Client {
        endpointUrl = Url.parse("http://s3.localhost.localstack.cloud:4566")
        region = "us-east-1"
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = "x"
            secretAccessKey = "x"
        }
    }

val debeziumProperties = Properties().apply {
    putAll(
        mapOf(
            "connector.class" to "io.debezium.connector.postgresql.PostgresConnector",
            "name" to "orders",
            "offset.storage" to "org.apache.kafka.connect.storage.MemoryOffsetBackingStore",
            "schema.history.internal" to "io.debezium.relational.history.MemorySchemaHistory",
            "schemas.enable" to "false",
            "database.server.id" to "001",
            "database.hostname" to "localhost",
            "database.port" to "5432",
            "database.user" to "postgresql",
            "database.password" to "postgresql",
            "database.dbname" to "orders",
            "topic.prefix" to "orders",
            "max.batch.size" to "500",
            "max.queue.size" to "1000",
            "include.schema.changes" to "false",
            "database.allowPublicKeyRetrieval" to "true",
            "plugin.name" to "pgoutput",
            "transforms.unwrap.type" to "io.debezium.transforms.ExtractNewRecordState",
            "key.converter" to "org.apache.kafka.connect.storage.StringConverter",
            "key.converter.schemas.enable" to "false",
            "value.converter" to "org.apache.kafka.connect.json.JsonConverter",
            "value.converter.schemas.enable" to "false",
            "transforms" to "unwrap"
        )
    )
}
