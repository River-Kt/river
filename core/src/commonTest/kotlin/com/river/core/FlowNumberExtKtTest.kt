package com.river.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

class FlowNumberExtKtTest : FunSpec({
    val oneMillionFlow = (1..1000000).asFlow()

    test("Flow<Int>.sum()") {
        oneMillionFlow
            .sum() shouldBe 500000500000
    }

    test("Flow<Long>.sum()") {
        oneMillionFlow
            .map { it.toLong() }
            .sum() shouldBe 500000500000
    }
})
