package io.github.gabfssilva.river.r2dbc

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.reactive.awaitFirst

class R2dbcFlowExtTest : FeatureSpec({
    feature("Test statement execution") {
        val connection = h2Client.awaitFirst()

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

val h2Client = ConnectionFactories.get(
    "r2dbc:h2:mem:///testdb;INIT=RUNSCRIPT%20FROM%20'src/test/resources/h2.sql'"
).create()