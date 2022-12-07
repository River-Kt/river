package io.github.gabfssilva.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

suspend fun Flow<Int>.sum(): Long =
    map { it.toLong() }.sum()
