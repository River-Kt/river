package com.river.core

//import java.nio.ByteBuffer
//
///**
// * Converts the [Flow] of [ByteBuffer] to a [Flow] of [ByteArray].
// *
// * @return A new [Flow] of [ByteArray] converted from the original [Flow] of [ByteBuffer].
// */
//fun Flow<ByteBuffer>.asByteArray(): Flow<ByteArray> =
//    map { bb -> ByteArray(bb.remaining()).also { bb.get(it) } }
//
///**
// * Flattens a [Flow] of [List] of [ByteBuffer] and converts it to a [Flow] of [ByteArray].
// *
// * @return A new [Flow] of [ByteArray] converted and flattened from the original [Flow] of [List] of [ByteBuffer].
// */
//fun Flow<List<ByteBuffer>>.flattenAsByteArray(): Flow<ByteArray> =
//    flattenIterable().asByteArray()
