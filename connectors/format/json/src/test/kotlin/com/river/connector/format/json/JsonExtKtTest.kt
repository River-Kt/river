package com.river.connector.format.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.IntNode
import com.river.core.asByteArray
import com.river.core.flatten
import com.river.core.intersperse
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.*

class JsonExtKtTest : FeatureSpec({
    feature("Json stream processing") {
        scenario("Object to json") {
            val flow = flowOf(1, 2, 3)
            val jsonFlow: Flow<JsonNode> = flow.asJsonNode()

            jsonFlow.toList() shouldContainInOrder listOf(
                IntNode(1), IntNode(2), IntNode(3)
            )
        }

        scenario("Object to json string") {
            val flow = flowOf(1, 2, 3)
            val jsonFlow: Flow<String> = flow.asJsonString()

            jsonFlow shouldContainInOrder flowOf(
                "1", "2", "3"
            )
        }

        scenario("Json string to object") {
            val flow = flowOf("1", "2", "3")
            val jsonFlow: Flow<String> = flow.asParsedJson()

            jsonFlow shouldContainInOrder flowOf(
                "1", "2", "3"
            )
        }

        scenario("Json node to object") {
            val flow = flowOf("1", "2", "3").asJsonNode()
            val jsonFlow: Flow<String> = flow.asValue()

            jsonFlow shouldContainInOrder flowOf(
                "1", "2", "3"
            )
        }

        scenario("Streamed byte array to json object") {
            val jsonFlow = flowOf("1", "2", "3", "4", "5").intersperse(
                start = "[", between = ",", end = "]"
            ).map { it.toByteArray().toList() }.flatten().map { listOf(it).toByteArray() }
                .parseJsonArray<JsonNode>()

            jsonFlow shouldContainInOrder (1..5).map { IntNode(it) }.asFlow()
        }

        scenario("Irregular json emission") {
            flowOf("[", "1,", "2", "", " ", ",", "3", ",4,5", ",", "    ", "6", " ]").asByteArray()
                .parseJsonArray<Int>() shouldContainInOrder flowOf(1, 2, 3, 4, 5, 6)
        }

        data class Person(val name: String, val age: Int)

        scenario("Irregular json object emission") {
            val jsonBytesFlow = flowOf(
                "[",
                """{"name":"Alice"    ,  """,
                """"age":30""",
                "},  ",
                """    {"name":"Bob"""",
                ""","age":                   25}""",
                "]"
            ).asByteArray()

            jsonBytesFlow.parseJsonArray<Person>() shouldContainInOrder flowOf(
                Person("Alice", 30),
                Person("Bob", 25),
            )
        }

        scenario("Simple JSON lines") {
            val jsonBytesFlow = flowOf(
                """{ "name": "Alice", "age": 30 }""", """{ "name": "Bob", "age": 25 }"""
            ).intersperse("\n").asByteArray()

            jsonBytesFlow.parseJsonLines<Person>() shouldContainInOrder flowOf(
                Person("Alice", 30),
                Person("Bob", 25),
            )
        }

        fun String.jsonTree() = defaultObjectMapper.readTree(this)

        scenario("Chunked JSON lines") {
            val data = """
                {"name": "Gilbert", "wins": [["straight", "7♣"], ["one pair", "10♥"]]}
                {"name": "Alexa", "wins": [["two pair", "4♠"], ["two pair", "9♠"]]}
                {"name": "May", "wins": []}
                {"name": "Deloise", "wins": [["three of a kind", "5♣"]]}
            """

            flowOf(data).asByteArray().parseJsonLines<JsonNode>() shouldContainInOrder flowOf(
                """{"name": "Gilbert", "wins": [["straight", "7♣"], ["one pair", "10♥"]]}""".jsonTree(),
                """{"name": "Alexa", "wins": [["two pair", "4♠"], ["two pair", "9♠"]]}""".jsonTree(),
                """{"name": "May", "wins": []}""".jsonTree(),
                """{"name": "Deloise", "wins": [["three of a kind", "5♣"]]}""".jsonTree()
            )
        }

        scenario("Irregular JSON lines emission") {
            flowOf(
                """{"name": "Gilbert", """,
                """"wins": [["straight", "7♣"], ["one pair", "10♥"]]}""",
                "\n",
                """{"name": "Alexa", "wins": [["two pair", "4♠"]""",
                ", ",
                """["two pa""",
                """ir", "9♠"]]}""",
                "\n",
                """{"name": "May", "wins": [""",
                """]}
                {"name": "Del""",
                """oise", "wins": [["three of a kind", "5♣"]]}"""
            ).asByteArray().parseJsonLines<JsonNode>() shouldContainInOrder flowOf(
                """{"name": "Gilbert", "wins": [["straight", "7♣"], ["one pair", "10♥"]]}""".jsonTree(),
                """{"name": "Alexa", "wins": [["two pair", "4♠"], ["two pair", "9♠"]]}""".jsonTree(),
                """{"name": "May", "wins": []}""".jsonTree(),
                """{"name": "Deloise", "wins": [["three of a kind", "5♣"]]}""".jsonTree()
            )
        }
    }
})

suspend infix fun <T> Flow<T>.shouldContainInOrder(
    other: Flow<T>
) = zip(other) { actual, expected -> actual shouldBe expected }.collect()
