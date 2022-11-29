package io.github.gabfssilva.river.r2dbc

import io.kotest.core.spec.style.FeatureSpec
import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirst

class R2dbcFlowExtTest : FeatureSpec({
    feature("Test statement execution") {
        val connection = h2Client.awaitFirst()

        scenario("Insert statements") {
            val flow = (1..10)
                .asFlow()
                .map {
                    connection.createStatement("INSERT INTO test (id) VALUES ('$it')")
                }

            connection.executeStatementFlow(flow).collect(::println)
        }

        scenario("Select statements") {
            val flow = (1..10)
                .asFlow()
                .map {
                    connection.createStatement("SELECT * FROM test WHERE id = '$it'")
                }

            connection.executeStatementFlow(flow).collect(::println)
        }

        scenario("Update statements") {
            val flow = (1..10)
                .asFlow()
                .map {
                    connection.createStatement("UPDATE test set id = '$it' WHERE id = '$it'")
                }

            connection.executeStatementFlow(flow).collect(::println)
        }

        scenario("Delete statements") {
            val flow = (1..10)
                .asFlow()
                .map {
                    connection.createStatement("DELETE FROM test WHERE id = '$it'")
                }

            connection.executeStatementFlow(flow).collect(::println)
        }
    }
})

val h2Client = ConnectionFactories.get(
    "r2dbc:h2:mem:///testdb;INIT=RUNSCRIPT%20FROM%20'src/test/resources/h2.sql'"
).create()