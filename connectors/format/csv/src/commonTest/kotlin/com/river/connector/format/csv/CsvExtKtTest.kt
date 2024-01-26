package com.river.connector.format.csv

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectIndexed

class CsvExtKtTest : FunSpec({
    test("CSV generation: raw csv") {
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
})
