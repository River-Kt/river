package com.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Converts the [Flow] of [ByteArray] to a [Flow] of [String].
 *
 * @return A new [Flow] of [String] converted from the original [Flow] of [ByteArray].
 */
fun Flow<ByteArray>.asString(): Flow<String> =
    map { it.decodeToString() }
