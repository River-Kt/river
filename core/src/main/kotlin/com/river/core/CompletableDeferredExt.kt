package com.river.core

import kotlinx.coroutines.CompletableDeferred

suspend fun <T> withPromise(f: suspend (CompletableDeferred<T>) -> Unit): T =
    CompletableDeferred<T>()
        .also { f(it)  }
        .await()
