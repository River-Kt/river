package com.river.core

import app.cash.turbine.test
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.retry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.comparables.shouldNotBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.mpp.atomics.AtomicReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
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
        val sourceFlow =
            (1..100)
                .asFlow()
                .mapAsync(10) {
                    delay(500)
                    it * 2
                }

        // The high max retries is due to macOS incredibly slow build on GH Actions
        // It causes the test to slow down, so time-related tests get quite unpredictable
        retry(20, 1.minutes) {
            val counter = AtomicReference(0)

            sourceFlow
                .test {
                    (1..10)
                        .forEach { _ ->
                            suspend fun ensureNext(): Duration {
                                counter.increment()
                                val (item, duration) = measureTimedValue { awaitItem() }
                                item shouldBe counter.value * 2
                                return duration
                            }

                            // Measure the time taken to receive two items
                            measureTime {
                                // First item should be received in >= 500 ms & <= 600 ms
                                assertSoftly(ensureNext()) { duration ->
                                    duration shouldBeGreaterThan 480.milliseconds
                                    // Once again, this is due to macOS slow builds on GH Actions
                                    duration shouldBeLessThanOrEqualTo 600.milliseconds
                                }

                                repeat(9) {
                                    // The other 9 items should be almost immediate
                                    ensureNext() shouldNotBeGreaterThan 10.milliseconds
                                }

                            } shouldBeLessThanOrEqualTo 650.milliseconds
                        }

                    awaitComplete()
                }
        }
    }

    test("unorderedMapAsync should correctly transform each element") {
        val sourceFlow = flowOf(500, 100, 5)

        val result =
            sourceFlow
                .unorderedMapAsync(2) {
                    delay(it.milliseconds)
                    it * 2
                }
                .toList()

        /**
         * The assertion shouldContainInOrder expects the elements in the specific order [200, 10, 1000].
         *
         * Despite the function being unordered, in this specific case, the output will be ordered due to the
         * interplay of processing times and concurrency limit.
         *
         * Lemme explain how it works exactly:
         *
         *  - The first element (500) will start processing and take 500 milliseconds.
         *  - Meanwhile, the second element (100) starts and finishes in 100 milliseconds.
         *  - The third element (5) starts processing after the second but finishes quickly in 5 milliseconds.
         *  - By the time the first element (500) finishes, the other two are already done.
         *
         *  So, the order of completion is 100 (200 after transformation), 5 (10 after transformation),
         *  and finally 500 (1000 after transformation).
         */
        result shouldContainInOrder listOf(200, 10, 1000)
    }

    test("flatMapIterableAsync should correctly transform and flatten each element") {
        val sourceFlow = flowOf(1, 2, 3)

        val result = sourceFlow
            .flatMapIterableAsync(2) { value ->
                delay(50 * value.toLong()) // Delay to simulate asynchronous processing
                listOf(value, value + 1)   // Transform each item into an iterable
            }
            .toList() // Collect the results into a List

        // The expected result is a flattened list of transformed items
        // Since the flow is ordered, despite the concurrency, the order of elements is maintained
        result shouldContainInOrder listOf(1, 2, 2, 3, 3, 4)
    }

    test("unorderedFlatMapIterableAsync should correctly transform and flatten each element with specific processing times") {
        val sourceFlow = flowOf(500, 100, 5)

        val result =
            sourceFlow
                .unorderedFlatMapIterableAsync(2) {
                    delay(it.milliseconds)
                    listOf(it, it * 2)
                }
                .toList()

        /**
         * The assertion shouldContainInOrder expects the elements in the specific order [100, 200, 5, 10, 500, 1000].
         *
         * Despite the function being unordered, in this specific case, the output will be ordered due to the
         * interplay of processing times and concurrency limit.
         *
         * Lemme explain how it works exactly:
         *
         *  - The first element (500) will start processing and take 500 milliseconds.
         *  - Meanwhile, the second element (100) starts and finishes in 100 milliseconds.
         *  - The third element (5) starts processing after the second but finishes quickly in 5 milliseconds.
         *  - By the time the first element (500) finishes, the other two are already done.
         *
         *  So, the order of completion is 100 (200 after transformation), 5 (10 after transformation),
         *  and finally 500 (1000 after transformation).
         */
        result shouldContainInOrder listOf(100, 200, 5, 10, 500, 1000)
    }
})
