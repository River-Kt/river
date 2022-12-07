package io.github.gabfssilva.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import java.math.BigDecimal

suspend fun Flow<BigDecimal>.sum(): BigDecimal =
    fold(BigDecimal.ZERO) { acc, i -> acc + i }
