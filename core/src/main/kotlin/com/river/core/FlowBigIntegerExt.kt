package com.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import java.math.BigInteger

suspend fun Flow<BigInteger>.sum(): BigInteger =
    fold(BigInteger.ZERO) { acc, i -> acc + i }
