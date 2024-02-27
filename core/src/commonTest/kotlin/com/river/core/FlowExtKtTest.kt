@file:OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class, ExperimentalRiverApi::class)

package com.river.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldNotContainAll
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class FlowExtKtTest : FunSpec({
    val infiniteFlow = poll { listOf("hello!") }

    test("Assert that using cancel() is working properly and the flow no longer emits new values") {
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

    test("Assert that using other exceptions will make any collecting throw the error") {
        val stoppableFlow =
            stoppableFlow {
                var counter = 0

                infiniteFlow
                    .collect {
                        emit(++counter)
                        if (counter == 1000) error("ups, you should use `cancel` instead")
                    }
            }

        runCatching { stoppableFlow.collect() } shouldBeFailure {
            it.message shouldBe "ups, you should use `cancel` instead"
        }
    }

    test("should transform the flow into a stoppable & complete it after the predicate returns true") {
        val maybePrimes = flowOf(2, 4, 6, 8, 10, 12, 13, 14, 16)

        maybePrimes
            .earlyCompleteIf { it % 2 != 0 }
            .toList()
            .toList() shouldBe listOf(2, 4, 6, 8, 10, 12)
    }

    test("should transform the flow into a list") {
        flowOf(2, 4, 6, 8, 10, 12, 14, 16)
            .toList() shouldBe listOf(2, 4, 6, 8, 10, 12, 14, 16)
    }

    test("should transform the flow into a list based on the count") {
        flowOf(2, 4, 6, 8, 10, 12, 14, 16)
            .toList(3) shouldBe listOf(2, 4, 6)
    }

    test("should transform the flow into a list based on the time window") {
        flowOf(2, 4, 6, 8, 10, 12, 14, 16)
            .onEach { delay(300) }
            .toList(10, 1150.milliseconds) shouldBe listOf(2, 4, 6)
    }

    test("should chunk the items based on the count") {
        flowOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
            .chunked(10)
            .toList() shouldBe listOf(listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20))
    }

    test("should chunk the items based on the time window") {
        flowOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
            .onEach { delay(250.milliseconds) }
            .chunked(10, 400.milliseconds)
            .toList() shouldBe listOf(listOf(2, 4), listOf(6, 8), listOf(10, 12), listOf(14, 16), listOf(18, 20))
    }

    test("Should transform the Flow<List<T>> into a Flow<T>") {
        val flowOfList = flowOf(listOf(2, 4), listOf(6, 8), listOf(10, 12), listOf(14, 16), listOf(18, 20))
        val flattenedFlow = flowOfList.flattenIterable()
        flattenedFlow.toList() shouldBe listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
    }

    test("Should allow a concurrency for item processing within the flow") {
        val (_, duration) = measureTimedValue {
            flowOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
                .mapAsync(5) { delay(500.milliseconds) }
                .collect()
        }

        duration.inWholeSeconds shouldBe 1
    }

    val flow =
        (1..20)
            .asFlow()

    test("Should suspend after the defined number of elements per time window is reached") {
        val (result, duration) = measureTimedValue { flow.throttle(10, 2.seconds).toList() }

        duration.inWholeSeconds shouldBe 4
        result shouldContainInOrder (1..20).toList()
    }

    test("Should drop if specified after the defined number of elements per time window is reached") {
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
})
