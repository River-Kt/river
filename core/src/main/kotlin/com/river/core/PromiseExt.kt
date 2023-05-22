package com.river.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

suspend fun <T> withPromise(f: suspend (CompletableDeferred<T>) -> Unit): T =
    CompletableDeferred<T>()
        .also { f(it)  }
        .await()

fun <T> promiseFlow(f: suspend (CompletableDeferred<Flow<T>>) -> Unit): Flow<T> =
    flow {
        val flow: Flow<T> = withPromise { f(it) }
        emitAll(flow)
    }
