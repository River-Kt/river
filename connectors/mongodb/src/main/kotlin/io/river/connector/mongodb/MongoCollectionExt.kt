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

fun <T> MongoCollection<T>.insert(
    flow: Flow<T>,
    parallelism: Int = 1,
): Flow<InsertOneResult> =
    flow.mapParallel(parallelism) { insertOne(it).awaitFirst() }

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

fun MongoCollection<Document>.update(
    flow: Flow<Document>,
    filter: Bson,
    parallelism: Int = 1,
) = flow.mapParallel(parallelism) { updateOne(filter, it).awaitFirst() }

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

fun <T> MongoCollection<T>.replace(
    flow: Flow<T>,
    filter: Bson,
    parallelism: Int = 1,
): Flow<UpdateResult> = flow.mapParallel(parallelism) { replaceOne(filter, it).awaitFirst() }

fun <T> MongoCollection<T>.replace(
    flow: Flow<Pair<Bson, T>>,
    parallelism: Int = 1,
): Flow<UpdateResult> =
    flow.mapParallel(parallelism) { (filter, document) -> replaceOne(filter, document).awaitFirst() }

fun <T : Any> MongoCollection<T>.findAsFlow(
    query: Bson
): Flow<T> = find(query).asFlow()

fun <T : Any> MongoCollection<T>.findAsFlow(): Flow<T> =
    find().asFlow()
