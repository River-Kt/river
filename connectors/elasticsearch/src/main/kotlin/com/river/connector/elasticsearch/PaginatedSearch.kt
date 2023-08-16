package com.river.connector.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.search.Hit
import co.elastic.clients.util.ObjectBuilder
import com.river.core.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await

@ExperimentalRiverApi
@ExperimentalCoroutinesApi
sealed interface PaginatedSearch {
    fun <T> paginatedSearchFlow(
        client: ElasticsearchAsyncClient,
        clazz: Class<T>
    ): Flow<Hit<T>>

    class Default(
        val index: String,
        val pageSize: Int = 100,
        val concurrency: Int = 1,
        val f: (Query.Builder) -> ObjectBuilder<Query> = { it.matchAll { it } }
    ) : PaginatedSearch {

        override fun <T> paginatedSearchFlow(
            client: ElasticsearchAsyncClient,
            clazz: Class<T>
        ): Flow<Hit<T>> = flow {
            val maxWindow = client.maxResultWindow(index)

            val flow =
                indefinitelyRepeat(Unit)
                    .withIndex()
                    .map { it.index * pageSize }
                    .map { from ->
                        val maxSize =
                            if (from + pageSize <= maxWindow) pageSize
                            else maxWindow - from

                        if (maxSize <= 0) null
                        else {
                            SearchRequest.Builder()
                                .index(index)
                                .from(from)
                                .size(maxSize)
                                .query { f(it) }
                                .build()
                        }
                    }
                    .mapAsync(concurrency) { request ->
                        request?.let {
                            client.search(it, clazz)
                                .await()
                                .hits()
                                .hits()
                        } ?: emptyList()
                    }
                    .earlyCompleteIf { it.isEmpty() }
                    .flattenIterable()

            emitAll(flow)
        }
    }

    class BySearchAfter(
        val index: String,
        val pageSize: Int = 100,
        val concurrency: Int = 1,
        val fields: List<Field>,
        val f: (Query.Builder) -> ObjectBuilder<Query> = { it.matchAll { it } }
    ) : PaginatedSearch {
        class Field(val name: String, val order: SortOrder)

        override fun <T> paginatedSearchFlow(
            client: ElasticsearchAsyncClient,
            clazz: Class<T>
        ): Flow<Hit<T>> = flow {
            val baseBuilder =
                SearchRequest
                    .Builder()
                    .index(index)
                    .query { f(it) }

            fields.forEach { f -> baseBuilder.sort { b -> b.field { it.field(f.name).order(f.order) } } }

            val first =
                client.search(baseBuilder.build(), clazz)
                    .await()
                    .hits()
                    .hits()

            emitAll(first.asFlow())

            if (first.isNotEmpty()) {
                var lastHit = first.last().sort()

                val flow = poll(stopOnEmptyList = true) {
                    val (s, id) = lastHit

                    val req =
                        SearchRequest
                            .Builder()
                            .index(index)
                            .query { f(it) }
                            .sort { it.score { it.order(SortOrder.Asc) } }
                            .searchAfter {
                                it.longValue(s.longValue())
                                it.stringValue(id.stringValue())
                            }
                            .build()

                    client.search(req, clazz)
                        .await()
                        .hits()
                        .hits()
                        .also {
                            if (it.isNotEmpty()) {
                                lastHit = it.last().sort()
                            }
                        }
                }

                emitAll(flow)
            }
        }
    }
}
