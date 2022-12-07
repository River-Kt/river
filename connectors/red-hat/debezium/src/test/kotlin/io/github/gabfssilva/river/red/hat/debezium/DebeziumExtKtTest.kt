@file:OptIn(ExperimentalKotest::class)

package io.github.gabfssilva.river.red.hat.debezium

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.debezium.engine.DebeziumEngine.create
import io.debezium.engine.format.Json
import io.github.gabfssilva.river.core.*
import io.github.gabfssilva.river.rdbms.jdbc.Jdbc
import io.github.gabfssilva.river.rdbms.jdbc.singleUpdate
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.engine.test.logging.info
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.*
import java.util.*

class DebeziumExtKtTest : FeatureSpec({
    feature("Debezium CDC as flow") {
        scenario("Successful flow") {
            val jdbc = Jdbc(
                url = "jdbc:mysql://localhost:3306/db",
                connectionPoolSize = 10,
                credentials = "root" to "root"
            )

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

                val insertRows = (1..10)
                    .asFlow()
                    .map { """ { "counter": $it } """ }
                    .via { singleUpdate("insert into events(event_data) values(?);") { setString(1, it) } }

                runCatching { (dropTable + createTable + insertRows).sum() }
                    .onSuccess { info { "$it rows were updated" } }
                    .onFailure { it.printStackTrace() }
            }

            val objectMapper = ObjectMapper()

            debeziumFlow { create(Json::class.java).using(Properties().apply { putAll(config) }) }
                .onEach { it.markProcessed() }
                .map { objectMapper.readValue(it.record.value(), ObjectNode::class.java) }
                .map { it["payload"]["after"] }
                .take(10)
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
    "max.batch.size" to "2500",
    "max.queue.size" to "5000",
    "include.schema.changes" to "false"
)
