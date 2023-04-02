package com.river.connector.mongodb

import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoClients
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import com.river.core.ChunkStrategy
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.Document

class MongoCollectionExtTest : FeatureSpec({
    val client = MongoClients.create("mongodb://mongo:mongo@localhost:27017/")
    val db = client.getDatabase("sample")
    val collection = db.getCollection("numbers")

    beforeSpec {
        collection.drop().awaitFirstOrNull()
    }

    feature("MongoDB as flow") {
        val numberOfRecords = 1000

        scenario("Insert many records") {
            (1..numberOfRecords)
                .asFlow()
                .map { Document(mapOf("number" to it)) }
                .let { collection.insertMany(it, 1, chunkStrategy = ChunkStrategy.Count(1000)) }
                .collect()

            collection.countDocuments().awaitFirstOrNull() shouldBe numberOfRecords
        }

        scenario("Fetch element") {
            val elements =
                collection
                    .findAsFlow(Filters.lt("number", 10))
                    .map { it["number"] }
                    .toList()

            elements shouldContainInOrder (1..9).toList()
        }
    }
})
