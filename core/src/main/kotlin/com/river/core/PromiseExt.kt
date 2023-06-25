package com.river.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

suspend fun <T> withPromise(f: suspend (CompletableDeferred<T>) -> Unit): T =
    CompletableDeferred<T>()
        .also { f(it)  }
        .await()

suspend fun <T> CoroutineScope.withPromise(f: suspend (CompletableDeferred<T>) -> Unit): T =
    CompletableDeferred<T>()
        .also { launch { f(it) }  }
        .await()

fun <T> promiseFlow(f: suspend (CompletableDeferred<Flow<T>>) -> Unit): Flow<T> =
    flow {
        val flow: Flow<T> = withPromise { f(it) }
        emitAll(flow)
    }
