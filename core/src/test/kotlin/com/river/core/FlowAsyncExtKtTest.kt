package com.river.core

import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.comparables.shouldNotBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

class FlowAsyncExtKtTest : FunSpec({
    test("mapAsync should respect order & correctly transform each element") {
        val sourceFlow = flowOf(100, 50, 10)

        val result =
            sourceFlow
                .mapAsync(2) {
                    delay(it.milliseconds)
                    it * 2
                }
                .toList()

        // Since the flow is ordered, despite the concurrency, the order of elements is maintained
        result shouldContainInOrder listOf(200, 100, 20)
    }

    test("mapAsync should respect order & concurrency limit") {
        val sourceFlow = (1..100).asFlow()

        var counter = 0

        sourceFlow
            .mapAsync(2) {
                delay(100)
                it * 2
            }
            .test {
                (1..50)
                    .forEach { _ ->
                        suspend fun ensureNext(): Duration {
                            counter++
                            val (item, duration) = measureTimedValue { awaitItem() }
                            item shouldBe counter * 2
                            return duration
                        }

                        // Measure the time taken to receive two items
                        measureTime {
                            // First item should be received in <= 120 ms
                            ensureNext() shouldBeLessThanOrEqualTo 120.milliseconds
                            // Second item should be almost immediate
                            ensureNext() shouldNotBeGreaterThan 10.milliseconds
                        } shouldBeLessThanOrEqualTo 130.milliseconds  // Total time for both items should be <= 130 ms
                    }

                awaitComplete()
            }
    }

    test("unorderedMapAsync should correctly transform each element") {
        val sourceFlow = flowOf(100, 50, 10)

        val result =
            sourceFlow
                .unorderedMapAsync(2) {
                    delay(it.milliseconds)
                    it * 2
                }
                .toList()

        /**
         * The assertion shouldContainInOrder expects the elements in the specific order [100, 20, 200].
         *
         * Despite the function being unordered, in this specific case, the output will be ordered due to the
         * interplay of processing times and concurrency limit.
         *
         * Lemme explain how it works exactly:
         *
         *  - The first element (100) will start processing and take 100 milliseconds.
         *  - Meanwhile, the second element (50) starts and finishes in 50 milliseconds.
         *  - The third element (10) starts processing after the second but finishes quickly in 10 milliseconds.
         *  - By the time the first element (100) finishes, the other two are already done.
         *
         *  So, the order of completion is 50 (20 after transformation), 10 (20 after transformation),
         *  and finally 100 (200 after transformation).
         */
        result shouldContainInOrder listOf(100, 20, 200)
    }

    test("flatMapIterableAsync should correctly transform and flatten each element") {
        // Create a flow of integers
        val sourceFlow = flowOf(1, 2, 3)

        // Apply the flatMapIterableAsync extension function with a concurrency limit
        val result = sourceFlow
            .flatMapIterableAsync(2) { value ->
                delay(50 * value.toLong()) // Delay to simulate asynchronous processing
                listOf(value, value + 1)   // Transform each item into an iterable
            }
            .toList() // Collect the results into a List

        // The expected result is a flattened list of transformed items
        // Since the flow is ordered, despite the concurrency, the order of elements is maintained
        // Output should be: [1, 2, 2, 3, 3, 4]
        result shouldContainInOrder listOf(1, 2, 2, 3, 3, 4)
    }

    test("unorderedFlatMapIterableAsync should correctly transform and flatten each element with specific processing times") {
        val sourceFlow = flowOf(100, 50, 10)

        val result =
            sourceFlow
                .unorderedFlatMapIterableAsync(2) {
                    delay(it.milliseconds)
                    listOf(it, it * 2)
                }
                .toList()

        /**
         * The assertion shouldContainInOrder expects the elements in the specific order [100, 20, 200].
         *
         * Despite the function being unordered, in this specific case, the output will be ordered due to the
         * interplay of processing times and concurrency limit.
         *
         * Lemme explain how it works exactly:
         *
         *  - The first element (100) will start processing and take 100 milliseconds.
         *  - Meanwhile, the second element (50) starts and finishes in 50 milliseconds.
         *  - The third element (10) starts processing after the second but finishes quickly in 10 milliseconds.
         *  - By the time the first element (100) finishes, the other two are already done.
         *
         *  So, the order of completion is 50 (100 after transformation), 10 (20 after transformation),
         *  and finally 100 (200 after transformation).
         */
        result shouldContainInOrder listOf(50, 100, 10, 20, 100, 200)
    }
})
