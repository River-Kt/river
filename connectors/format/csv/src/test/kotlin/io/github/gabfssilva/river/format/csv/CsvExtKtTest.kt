package io.github.gabfssilva.river.format.csv

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.map

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

        scenario("reified csv generation") {
            data class Payment(
                val id: Long,
                val amount: Long,
                val status: String
            )

            (1..100L)
                .asFlow()
                .map { Payment(it, it * 2, if (it % 2 == 0L) "paid" else "not_paid") }
                .csv()
                .collectIndexed { index, value ->
                    if (index == 0) {
                        value shouldBe "id;amount;status"
                    } else {
                        value shouldBe "$index;${index * 2};${if (index % 2 == 0) "paid" else "not_paid"}"
                    }
                }
        }
    }
})
