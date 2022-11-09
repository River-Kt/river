@file:OptIn(ExperimentalTime::class)

package io.github.gabfssilva.river.core

import io.github.gabfssilva.river.core.internal.UnfoldFlow.ParallelismIncreaseStrategy.Companion.MaxAllowedAfterReceive
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.zip
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class FlowExtKtTest : FeatureSpec({
    feature("unfold | unfoldParallel: Flow<T>") {
        scenario("building a simple non-stopping flow") {
            val count = 100
            val counter = AtomicInteger()

            val flow = unfold { listOf(counter.incrementAndGet()) }
            val list = flow.take(count).toList()

            list shouldContainInOrder (1..count).toList()
            counter.get() shouldBe count
        }

        scenario("building a parallelized non-stopping flow") {
            val count = 1000
            val counter = AtomicInteger()

            val flow = unfoldParallel(maxParallelism = 5) { listOf(counter.incrementAndGet()) }
            val list = flow.take(count).toList()

            list shouldContainInOrder (1..count).toList()
            counter.get() shouldBe count
        }

        scenario("building a parallelized repeat flow that stops on empty lists") {
            val count = 1000
            val counter = AtomicInteger()

            val flow = unfoldParallel(maxParallelism = 5, stopOnEmptyList = true) {
                if (counter.get() == count) emptyList()
                else listOf(counter.incrementAndGet())
            }

            val list = flow.toList()

            list shouldContainInOrder (1..count).toList()
            counter.get() shouldBe count
        }

        scenario("asserting the increase strategy is working properly") {
            val counter = AtomicInteger()
            val maxParallelism = 10

            val gap = ((100..120) + (250..270) + (850..980))
            val changedParallelism = AtomicReference(1 to 0)

            val flow =
                unfoldParallel(maxParallelism = maxParallelism, increaseStrategy = MaxAllowedAfterReceive) {
                    val c = counter.incrementAndGet()

                    val (last, count) = changedParallelism.get()

                    if (last != currentParallelism) {
                        changedParallelism.set(currentParallelism to count + 1)
                    }

                    if (c in gap) emptyList()
                    else listOf(c)
                }

            val list = flow.earlyCompleteIf { it > 1000 }.toList()

            val expected = ((1..1000) - gap).toList()
            list shouldBe expected
            changedParallelism.get() shouldBe (10 to 7)
        }
    }

    feature("stoppableFlow") {
        val infiniteFlow = unfold { listOf("hello!") }

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
                .windowedChunk(10, 400.milliseconds)
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

    feature("Flow<T>.mapParallel") {
        scenario("Should allow a concurrency for item processing within the flow") {
            val (_, duration) = measureTimedValue {
                flowOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
                    .mapParallel(5) { delay(500.milliseconds) }
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
