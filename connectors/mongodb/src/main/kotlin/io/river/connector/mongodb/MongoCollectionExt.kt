@file:OptIn(FlowPreview::class)

package io.river.connector.mongodb

import com.mongodb.client.model.InsertManyOptions
import com.mongodb.client.result.InsertManyResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.MongoCollection
import io.river.core.ChunkStrategy
import io.river.core.chunked
import io.river.core.mapParallel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.bson.Document
import org.bson.conversions.Bson
import kotlin.time.Duration.Companion.milliseconds

/**
 * Inserts documents into a MongoDB collection using a flow of documents.
 *
 * @param flow The flow of documents to insert.
 * @param parallelism The parallelism for this operation. Defaults to 1.
 *
 * @return A flow of InsertOneResult objects.
 *
 * Example usage:
 *
 * ```
 *  val collection = // new MongoCollection<Document>
 *  val documentsFlow = flowOf(Document("field" to "value"))
 *  collection.insert(documentsFlow).collect { result -> println("Document inserted: ${result.insertedId}") }
 * ```
 */
fun <T> MongoCollection<T>.insert(
    flow: Flow<T>,
    parallelism: Int = 1,
): Flow<InsertOneResult> =
    flow.mapParallel(parallelism) { insertOne(it).awaitFirst() }

/**
 * Inserts many documents into a MongoDB collection using a flow of documents, batching them by the provided chunkStrategy.
 *
 * @param flow The flow of documents to insert.
 * @param parallelism The parallelism for this operation. Defaults to 1.
 * @param options The InsertManyOptions to use when inserting the documents. Defaults to InsertManyOptions().
 * @param chunkStrategy The strategy for chunking the documents. Defaults to ChunkStrategy.TimeWindow(10, 500.milliseconds).
 *
 * @return A flow of InsertManyResult objects.
 *
 * Example usage:
 *
 * ```
 *  val collection = // new MongoCollection<Document>
 *  val documentsFlow = flowOf(Document("field" to "value"))
 *  collection.insertMany(documentsFlow).collect { result -> println("Documents inserted: ${result.insertedIds.size}") }
 * ```
 */
fun <T> MongoCollection<T>.insertMany(
    flow: Flow<T>,
    parallelism: Int = 1,
    options: InsertManyOptions = InsertManyOptions(),
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 500.milliseconds)
): Flow<InsertManyResult> =
    flow
        .chunked(chunkStrategy)
        .mapParallel(parallelism) { insertMany(it, options).asFlow() }
        .flattenConcat()

/**
 * Updates documents in a MongoDB collection using a flow of update documents and a filter.
 *
 * @param flow The flow of update documents.
 * @param filter The BSON filter to apply when updating documents.
 * @param parallelism The parallelism for this operation. Defaults to 1.
 *
 * @return A flow of UpdateResult objects.
 *
 * Example usage:
 *
 * ```
 *  val collection = // new MongoCollection<Document>
 *  val filter = Filters.eq("field", "value")
 *  val updatesFlow = flowOf(Document("\$set" to Document("field" to "newValue")))
 *  collection.update(updatesFlow, filter).collect { result -> println("Documents updated: ${result.modifiedCount}") }
 * ```
 */
fun MongoCollection<Document>.update(
    flow: Flow<Document>,
    filter: Bson,
    parallelism: Int = 1,
) = flow.mapParallel(parallelism) { updateOne(filter, it).awaitFirst() }

/**
 * Updates many documents in a MongoDB collection using a flow of update documents, a filter, and batching by the provided chunkStrategy.
 *
 * @param flow The flow of update documents.
 * @param filter The BSON filter to apply when updating documents.
 * @param parallelism The parallelism for this operation. Defaults to 1.
 * @param chunkStrategy The strategy for chunking the update documents. Defaults to ChunkStrategy.TimeWindow(10, 500.milliseconds).
 *
 * @return A flow of UpdateResult objects.
 *
 * Example usage:
 *
 * ```
 *  val collection = // new MongoCollection<Document>
 *  val filter = Filters.eq("field", "value")
 *  val updatesFlow = flowOf(Document("\$set" to Document("field" to "newValue")))
 *  collection.updateMany(updatesFlow, filter).collect { result -> println("Documents updated: ${result.modifiedCount}") }
 * ```
 */
fun MongoCollection<Document>.updateMany(
    flow: Flow<Document>,
    filter: Bson,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 500.milliseconds)
): Flow<UpdateResult> =
    flow
        .chunked(chunkStrategy)
        .mapParallel(parallelism) { updateMany(filter, it).asFlow() }
        .flattenConcat()

/**
 * Replaces documents in a MongoDB collection using a flow of documents and a filter.
 *
 * @param flow The flow of documents to replace.
 * @param filter The BSON filter to apply when replacing documents.
 * @param parallelism The parallelism for this operation. Defaults to 1.
 *
 * @return A flow of UpdateResult objects.
 *
 * Example usage:
 *
 * ```
 *  val collection = // new MongoCollection<Document>
 *  val filter = Filters.eq("field", "value")
 *  val replacementsFlow = flowOf(Document("field" to "newValue"))
 *  collection.replace(replacementsFlow, filter).collect { result -> println("Documents replaced: ${result.modifiedCount}") }
 * ```
 */
fun <T> MongoCollection<T>.replace(
    flow: Flow<T>,
    filter: Bson,
    parallelism: Int = 1,
): Flow<UpdateResult> = flow.mapParallel(parallelism) { replaceOne(filter, it).awaitFirst() }

/**
 * Replaces documents in a MongoDB collection using a flow of pairs containing a filter and a document.
 *
 * @param flow The flow of pairs containing a filter and a document.
 * @param parallelism The parallelism for this operation. Defaults to 1.
 *
 * @return A flow of UpdateResult objects.
 *
 * Example usage:
 *
 * ```
 *  val collection = // new MongoCollection<Document>
 *  val filter = Filters.eq("field", "value")
 *  val replacementsFlow = flowOf(filter to Document("field" to "newValue"))
 *  collection.replace(replacementsFlow).collect { result -> println("Documents replaced: ${result.modifiedCount}") }
 * ```
 */
fun <T> MongoCollection<T>.replace(
    flow: Flow<Pair<Bson, T>>,
    parallelism: Int = 1,
): Flow<UpdateResult> =
    flow.mapParallel(parallelism) { (filter, document) -> replaceOne(filter, document).awaitFirst() }

/**
 * Finds documents in a MongoDB collection that match the specified BSON query and returns them as a flow.
 *
 * @param query The BSON query to filter the documents.
 *
 * @return A flow of documents that match the specified BSON query.
 *
 * Example usage:
 *
 * ```
 *  val collection = // new MongoCollection<Document>
 *  val query = Filters.eq("field", "value")
 *  val resultsFlow = collection.findAsFlow(query)
 *  resultsFlow.collect { document -> println("Found document: $document") }
 * ```
 */
fun <T : Any> MongoCollection<T>.findAsFlow(
    query: Bson
): Flow<T> = find(query).asFlow()

/**
 * Finds all documents in a MongoDB collection and returns them as a flow.
 *
 * @return A flow of all documents in the collection.
 *
 * Example usage:
 *
 * ```
 *  val collection = // new MongoCollection<Document>
 *  val resultsFlow = collection.findAsFlow()
 *  resultsFlow.collect { document -> println("Found document: $document") }
 * ```
 */
fun <T : Any> MongoCollection<T>.findAsFlow(): Flow<T> =
    find().asFlow()
