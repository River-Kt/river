package com.river.connector.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem
import com.river.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlin.time.Duration.Companion.milliseconds

@ExperimentalRiverApi
fun <T> Flow<T>.toDocument(
    f: (T) -> Pair<String, String>
): Flow<Document<T>> =
    map {
        val (id, index) = f(it)
        Document(id, index, it)
    }

@ExperimentalRiverApi
inline fun <reified T> ElasticsearchAsyncClient.paginatedSearchFlow(
    configuration: PaginatedSearch
) = configuration.paginatedSearchFlow(this, T::class.java)

@ExperimentalRiverApi
suspend fun ElasticsearchAsyncClient.maxResultWindow(
    index: String,
    default: Int = 10000
) = indices()
    .getSettings { it.index(index) }
    .await()
    .result()[index]
    ?.settings()
    ?.maxResultWindow() ?: default

/**
 * This function is used to index documents asynchronously into Elasticsearch using bulk requests.
 * It takes a flow of Document<T> objects as input, along with optional parameters for concurrency and chunking.
 *
 * @param upstream A Flow of Document<T> objects to be indexed asynchronously into Elasticsearch
 * @param concurrency Optional parameter that specifies the number of concurrent bulk requests that can be executed at a time. Default value is 1.
 * @param groupStrategy Optional parameter that specifies the ChunkStrategy to be used for splitting the input stream into chunks. Default value is TimeWindow(100, 250.milliseconds).
 *
 * @return A Flow of BulkResponseItem objects that represent the results of indexing each individual document
 *
 * Example usage:
 *
 * ```
 *  val documents = listOf(
 *      Document("index1", "id1", mapOf("field1" to "value1")),
 *      Document("index1", "id2", mapOf("field1" to "value2")),
 *      Document("index2", "id3", mapOf("field1" to "value3"))
 *  ).asFlow()
 *
 *  ElasticsearchAsyncClient.indexFlow(documents, concurrency = 3).collect {
 *      when (it) {
 *          is BulkResponseItem.Failure -> println("Indexing failed for item with id ${it.id}: ${it.error}")
 *          is BulkResponseItem.Success -> println("Indexing succeeded for item with id ${it.id}")
 *      }
 *  }
 * ```
 */
@ExperimentalRiverApi
fun <T> ElasticsearchAsyncClient.indexFlow(
    upstream: Flow<Document<T>>,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(100, 250.milliseconds)
): Flow<BulkResponseItem> =
    upstream
        .chunked(groupStrategy)
        .mapAsync(concurrency) { chunk ->
            BulkRequest.Builder()
                .also { builder ->
                    chunk.forEach { document ->
                        builder.operations { b ->
                            b.index {
                                it.index(document.index)
                                    .id(document.id)
                                    .document(document.document)
                            }
                        }
                    }
                }
                .let { bulk(it.build()).await().items() }
        }
        .flattenIterable()
