@file:OptIn(ExperimentalKotest::class)

package com.river.connector.red.hat.debezium

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.debezium.engine.DebeziumEngine.create
import io.debezium.engine.format.Json
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.engine.test.logging.info
import io.kotest.matchers.shouldBe
import com.river.connector.rdbms.jdbc.Jdbc
import com.river.connector.rdbms.jdbc.singleUpdate
import com.river.core.catchAndEmitLast
import com.river.core.plus
import com.river.core.sum
import kotlinx.coroutines.flow.*
import java.util.*

class DebeziumExtKtTest : FeatureSpec({
    val objectMapper = ObjectMapper()

    feature("Debezium CDC as flow") {
        scenario("Successful flow") {
            val jdbc = Jdbc(
                url = "jdbc:mysql://localhost:3306/db",
                connectionPoolSize = 10,
                credentials = "root" to "root"
            )

            val numberOfRecords = 1000

            with(jdbc) {
                val dropTable =
                    singleUpdate(sql = "DROP TABLE events;")
                        .catchAndEmitLast { 0 }

                val createTable = singleUpdate(
                    sql = """
                        CREATE TABLE events(
                            id SERIAL PRIMARY KEY,
                            event_data JSON
                        );
                    """.trimIndent()
                )

                val insertRows = (1..numberOfRecords)
                    .asFlow()
                    .map { """ { "counter": $it } """ }
                    .let {
                        singleUpdate("insert into events(event_data) values(?);", it) {
                            setString(1, it)
                        }
                    }

                runCatching { (dropTable + createTable + insertRows).sum() }
                    .onSuccess { info { "$it rows were updated" } }
                    .onFailure { it.printStackTrace() }
            }

            debeziumFlow { create(Json::class.java).using(Properties().apply { putAll(config) }) }
                .onEach { it.markProcessed() }
                .map { objectMapper.readValue(it.record.value(), ObjectNode::class.java) }
                .map { it["payload"]["after"] }
                .take(numberOfRecords)
                .collectIndexed { index, row ->
                    val i = index + 1

                    row["id"].intValue() shouldBe i
                    row["event_data"].textValue() shouldBe "{\"counter\": $i}"
                }
        }
    }
})

val config = mapOf(
    "connector.class" to "io.debezium.connector.mysql.MySqlConnector",
    "name" to "events",
    "offset.storage" to "org.apache.kafka.connect.storage.MemoryOffsetBackingStore",
    "schema.history.internal" to "io.debezium.relational.history.MemorySchemaHistory",
    "schemas.enable" to "false",
    "database.server.id" to "001",
    "database.hostname" to "localhost",
    "database.port" to "3306",
    "database.user" to "root",
    "database.password" to "root",
    "topic.prefix" to "events",
    "max.batch.size" to "500",
    "max.queue.size" to "1000",
    "include.schema.changes" to "false",
    "database.allowPublicKeyRetrieval" to "true"
)
