package com.river.connector.format.csv

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectIndexed

class CsvExtKtTest : FeatureSpec({
    feature("CSV generation") {
        scenario("raw csv") {
            val numbers = (1..100).asFlow()

            numbers
                .rawCsv("n", "n * 2") { listOf("$it", "${it * 2}") }
                .collectIndexed { index, value ->
                    if (index == 0) {
                        value shouldBe "n;n * 2"
                    } else {
                        value shouldBe "$index;${index * 2}"
                    }
                }
        }
    }
})
