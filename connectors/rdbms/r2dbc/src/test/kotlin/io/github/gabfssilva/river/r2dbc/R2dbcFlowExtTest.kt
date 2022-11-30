package io.github.gabfssilva.river.r2dbc

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirst

class R2dbcFlowExtTest : FeatureSpec({
    feature("Test statement execution") {
        val connection = h2Client.awaitFirst()

        scenario("Test all type of statements") {

            forAll(
                row("INSERT INTO test (id) VALUES ('%s')"),
                row("SELECT * FROM test WHERE id = '%s'"),
                row("UPDATE test set id = id WHERE id = '%s'"),
                row("DELETE FROM test WHERE id = '%s'")
            ) { statement: String ->

                val flow = (1..10)
                    .asFlow()
                    .map {
                        connection.createStatement(statement.format(it))
                    }

                val result = connection.executeStatementFlow(flow)

                result.count() shouldBe 10
                result.shouldBeInstanceOf<Flow<Result>>()
            }
        }
    }
})

val h2Client = ConnectionFactories.get(
    "r2dbc:h2:mem:///testdb;INIT=RUNSCRIPT%20FROM%20'src/test/resources/h2.sql'"
).create()