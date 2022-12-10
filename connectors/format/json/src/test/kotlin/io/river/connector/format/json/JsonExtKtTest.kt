package io.river.connector.format.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.IntNode
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.river.core.flatten
import io.river.core.intersperse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

class JsonExtKtTest : FeatureSpec({
    feature("Json stream processing") {
        scenario("Object to json") {
            val flow = flowOf(1, 2, 3)
            val jsonFlow: Flow<JsonNode> = flow.toJson()

            jsonFlow.toList() shouldContainInOrder listOf(
                IntNode(1),
                IntNode(2),
                IntNode(3)
            )
        }

        scenario("Object to json string") {
            val flow = flowOf(1, 2, 3)
            val jsonFlow: Flow<String> = flow.toJsonString()

            jsonFlow.toList() shouldContainInOrder listOf(
                "1", "2", "3"
            )
        }

        scenario("Json string to object") {
            val flow = flowOf("1", "2", "3")
            val jsonFlow: Flow<String> = flow.fromJsonString()

            jsonFlow.toList() shouldContainInOrder listOf(
                "1", "2", "3"
            )
        }

        scenario("Json node to object") {
            val flow = flowOf("1", "2", "3").toJson()
            val jsonFlow: Flow<String> = flow.fromJson()

            jsonFlow.toList() shouldContainInOrder listOf(
                "1", "2", "3"
            )
        }

        scenario("Streamed byte array to json object") {
            val jsonFlow =
                flowOf("1", "2", "3", "4", "5")
                    .intersperse(
                        start = "[",
                        between = ",",
                        end = "]"
                    )
                    .map { it.toByteArray().toList() }
                    .flatten()
                    .map { listOf(it).toByteArray() }
                    .rootJsonNodes()

            jsonFlow.toList() shouldContainInOrder listOf(
                IntNode(1),
                IntNode(2),
                IntNode(3),
                IntNode(4),
                IntNode(5)
            )
        }
    }
})
