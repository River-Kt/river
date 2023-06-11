package com.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold

suspend fun Flow<Short>.sum(): Long =
    fold(0L) { acc, i -> acc + i }
