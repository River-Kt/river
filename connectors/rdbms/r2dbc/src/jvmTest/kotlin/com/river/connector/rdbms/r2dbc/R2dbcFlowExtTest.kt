package com.river.connector.rdbms.r2dbc

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.awaitFirst

class R2dbcFlowExtTest : FeatureSpec({
    feature("Test statement execution") {
        val connection =
            ConnectionFactories
                .get("r2dbc:h2:mem:///testdb")
                .create()
                .awaitFirst()

        beforeTest {
            connection
                .singleUpdate(
                    """DROP TABLE IF EXISTS books;
                    CREATE TABLE books (
                        id int auto_increment,
                        name varchar not null
                    )""".trimIndent()
                ).collect()
        }

        scenario("Single update flow") {
            val size = 10

            connection.singleUpdate(
                sql = "insert into books (name) values ($1)",
                upstream = (1..size).asFlow()
            ) { bind("$1", "book $it") }
                .fold(0L) { acc, i -> acc + i } shouldBe size

            connection.singleUpdate("delete from books")
                .first() shouldBe size
        }

        scenario("Batch update flow") {
            val size = 10

            val batchUpdateFlow =
                connection
                    .batchUpdate(
                        sql = "insert into books (name) values (?)",
                        upstream = (1..size).asFlow()
                    ) { bind(0, "book $it") }

            batchUpdateFlow
                .mapRow { it["id"] }
                .toList() shouldContainInOrder (1..10).toList()

            connection
                .singleUpdate("delete from books")
                .first() shouldBe size
        }

        scenario("Query flow") {
            val size = 50

            connection.singleUpdate(
                sql = "insert into books (name) values ($1)",
                upstream = (1..size).asFlow()
            ) {
                bind("$1", "book $it")
            }.collect()

            connection.query("select count(1) from books")
                .map { it.values.first() as Long }
                .first() shouldBe size

            connection.query("select name from books order by id")
                .withIndex()
                .onEach { (index, row) ->
                    row["NAME"] shouldBe "book ${index + 1}"
                }
                .count() shouldBe size
        }
    }
})
