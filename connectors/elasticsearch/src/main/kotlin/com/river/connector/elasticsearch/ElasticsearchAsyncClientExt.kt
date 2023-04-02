@file:OptIn(FlowPreview::class, ExperimentalTime::class)

package com.river.connector.elasticsearch

import com.river.core.*

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

fun <T> Flow<T>.toDocument(
    f: (T) -> Pair<String, String>
): Flow<Document<T>> =
    map {
        val (id, index) = f(it)
        Document(id, index, it)
    }

inline fun <reified T> ElasticsearchAsyncClient.paginatedSearchFlow(
    configuration: PaginatedSearch
) = configuration.paginatedSearchFlow(this, T::class.java)

suspend fun ElasticsearchAsyncClient.maxResultWindow(
    index: String,
    default: Int = 10000
) = indices()
    .getSettings { it.index(index) }
    .await()
    .result()[index]
    ?.settings()
    ?.maxResultWindow() ?: default

fun <T> ElasticsearchAsyncClient.indexFlow(
    upstream: Flow<Document<T>>,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(100, 250.milliseconds)
): Flow<BulkResponseItem> =
    upstream
        .chunked(chunkStrategy)
        .mapParallel(parallelism) { chunk ->
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
        .flatten()
