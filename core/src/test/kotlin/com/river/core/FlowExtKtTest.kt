@file:OptIn(ExperimentalTime::class)

package com.river.core

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class FlowExtKtTest : FeatureSpec({
    feature("stoppableFlow") {
        val infiniteFlow = poll { listOf("hello!") }

        scenario("Assert that using cancel() is working properly and the flow no longer emits new values") {
            val stoppableFlow =
                stoppableFlow {
                    var counter = 0

                    infiniteFlow
                        .collect {
                            emit(++counter)
                            if (counter == 1000) halt("counter reached the maximum count")
                        }
                }

            stoppableFlow.count() shouldBe 1000
        }

        scenario("Assert that using other exceptions will make any collecting throw the error") {
            val stoppableFlow =
                stoppableFlow {
                    var counter = 0

                    infiniteFlow
                        .collect {
                            emit(++counter)
                            if (counter == 1000) error("ups, you should use `cancel` instead")
                        }
                }

            assertThrows<IllegalStateException> { stoppableFlow.collect() }
        }
    }

    feature("Flow<T>.earlyCompleteIf") {
        scenario("should transform the flow into a stoppable & complete it after the predicate returns true") {
            val maybePrimes = flowOf(2, 4, 6, 8, 10, 12, 13, 14, 16)

            maybePrimes
                .earlyCompleteIf { it % 2 != 0 }
                .toList()
                .toList() shouldBe listOf(2, 4, 6, 8, 10, 12)
        }
    }

    feature("Flow<T>.asList") {
        scenario("should transform the flow into a list") {
            flowOf(2, 4, 6, 8, 10, 12, 14, 16)
                .toList() shouldBe listOf(2, 4, 6, 8, 10, 12, 14, 16)
        }

        scenario("should transform the flow into a list based on the count") {
            flowOf(2, 4, 6, 8, 10, 12, 14, 16)
                .toList(3) shouldBe listOf(2, 4, 6)
        }

        scenario("should transform the flow into a list based on the time window") {
            flowOf(2, 4, 6, 8, 10, 12, 14, 16)
                .onEach { delay(190) }
                .toList(10, 600.milliseconds) shouldBe listOf(2, 4, 6)
        }
    }

    feature("Flow<T>.chunked") {
        scenario("should chunk the items based on the count") {
            flowOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
                .chunked(10)
                .toList() shouldBe listOf(listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20))
        }

        scenario("should chunk the items based on the time window") {
            flowOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
                .onEach { delay(250.milliseconds) }
                .chunked(10, 400.milliseconds)
                .toList() shouldBe listOf(listOf(2, 4), listOf(6, 8), listOf(10, 12), listOf(14, 16), listOf(18, 20))
        }
    }

    feature("Flow<List<T>>.flatten") {
        scenario("Should transform the Flow<List<T>> into a Flow<T>") {
            val flowOfList = flowOf(listOf(2, 4), listOf(6, 8), listOf(10, 12), listOf(14, 16), listOf(18, 20))
            val flattenedFlow = flowOfList.flatten()
            flattenedFlow.toList() shouldBe listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
        }
    }

    feature("Flow<T>.mapAsync") {
        scenario("Should allow a concurrency for item processing within the flow") {
            val (_, duration) = measureTimedValue {
                flowOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
                    .mapAsync(5) { delay(500.milliseconds) }
                    .collect()
            }

            duration.inWholeSeconds shouldBe 1
        }
    }

    feature("Flow<T>.throttle") {
        val flow =
            (0..20)
                .asFlow()
                .delay(100.milliseconds)

        scenario("Should suspend after the defined number of elements per time window is reached") {
            val (result, duration) = measureTimedValue { flow.throttle(10, 2.seconds).toList() }

            duration.inWholeSeconds shouldBe 4
            result shouldContainInOrder (0..20).toList()
        }

        scenario("Should drop if specified after the defined number of elements per time window is reached") {
            val (result, duration) = measureTimedValue {
                flow
                    .throttle(
                        elementsPerInterval = 10,
                        interval = 2.seconds,
                        strategy = ThrottleStrategy.Drop
                    )
                    .toList()
            }

            duration.inWholeSeconds shouldBe 2
            result shouldNotContainAll (0..20).toList()
        }
    }

    feature("Flow<T>.broadcast") {
        val expectedSize = 100
        val stream = (1..expectedSize).asFlow()

        scenario("Broadcasting to two flows should emit the elements in order") {
            val (first, second) = stream.broadcast(2)

            val result =
                first
                    .map { it * 2 }
                    .zip(second.map { it * 3 }) { f, s -> f to s }
                    .toList()

            result shouldHaveSize expectedSize

            result
                .forEachIndexed { index, tuple ->
                    val (multipleOfTwo, multipleOfThree) = tuple

                    multipleOfTwo shouldBe (index + 1) * 2
                    multipleOfThree shouldBe (index + 1) * 3
                }
        }
    }
})
