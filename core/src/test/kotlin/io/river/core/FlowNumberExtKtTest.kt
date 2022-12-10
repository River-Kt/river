package io.river.core

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class FlowNumberExtKtTest : FeatureSpec({
    val oneMillionFlow = (1..1000000).asFlow()

    feature("Flow<Int> extensions") {
        scenario("Sum") {
            oneMillionFlow
                .sum() shouldBe 500000500000
        }
    }

    feature("Flow<Long> extensions") {
        scenario("Sum") {
            oneMillionFlow
                .map { it.toLong() }
                .sum() shouldBe 500000500000
        }
    }

    feature("Flow<BigDecimal> extensions") {
        scenario("Sum") {
            oneMillionFlow
                .map { BigDecimal(it) }
                .sum() shouldBe BigDecimal(500000500000)
        }
    }
})
