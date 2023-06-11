package com.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold

suspend fun Flow<Double>.sum(): Double =
    fold(0.0) { acc, i -> acc + i }
