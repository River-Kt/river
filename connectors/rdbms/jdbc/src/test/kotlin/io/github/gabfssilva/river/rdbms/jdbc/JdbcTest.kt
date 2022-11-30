package io.github.gabfssilva.river.rdbms.jdbc

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.*

class JdbcTest : FeatureSpec({
    feature("JDBC flow") {
        withJDBC {
            beforeTest {
                runCatching {
                    singleUpdate("DROP TABLE MESSAGES;")
                        .collect()
                }

                runCatching {
                    singleUpdate("CREATE TABLE MESSAGES(id serial PRIMARY KEY, body text NOT NULL);")
                        .collect()
                }
            }

            scenario("Single insert flow") {
                val size: Long = 10

                singleUpdate(
                    sql = "insert into messages (body) values (?)",
                    upstream = (1..size).asFlow()
                ) { setString(1, "message #$it") }
                    .count() shouldBe size

                query("select count(1) from messages;")
                    .map { it.values.first() as Long }
                    .first() shouldBe size
            }

            scenario("Batch insert flow") {
                val size = 1000

                batchUpdate(
                    sql = "insert into messages (body) values (?)",
                    upstream = (1..size).asFlow()
                ) { setString(1, "message #$it") }
                    .fold(0) { acc, i -> acc + i } shouldBe size

                query("select count(1) from messages;")
                    .map { it.values.first() as Long }
                    .first() shouldBe size
            }

            scenario("Query flow") {
                val size = 1000

                batchUpdate(
                    sql = "insert into messages (body) values (?)",
                    upstream = (1..size).asFlow()
                ) { setString(1, "message #$it") }
                    .collect()

                query("select count(1) from messages;")
                    .map { it.values.first() as Long }
                    .first() shouldBe size

                query("select body from messages order by id;")
                    .withIndex()
                    .onEach { (index, row) ->
                        row["body"] shouldBe "message #${index + 1}"
                    }
                    .count() shouldBe size
            }

            scenario("Typed query flow") {
                data class Message(val id: Long, val body: String)

                val size = 100

                batchUpdate(
                    sql = "insert into messages (body) values (?)",
                    upstream = (1..size).asFlow()
                ) { setString(1, "message #$it") }
                    .collect()

                typedQuery<Message>("select * from messages order by id;")
                    .withIndex()
                    .onEach { (index, row) ->
                        row.body shouldBe "message #${index + 1}"
                    }
                    .count() shouldBe size
            }
        }
    }
})

suspend inline fun withJDBC(
    f: Jdbc.() -> Unit
) {
    Jdbc(
        url = "jdbc:postgresql://localhost:5432/messages",
        credentials = "postgresql" to "postgresql",
        connectionPoolSize = 20
    ).also(f).also { it.close() }
}
