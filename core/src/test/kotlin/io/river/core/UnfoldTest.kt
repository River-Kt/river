package io.river.core

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class UnfoldTest : FeatureSpec({
    this as UnfoldTest

    feature("unfold | unfoldParallel: Flow<T>") {
        scenario("building a simple non-stopping flow") {
            val count = 100
            val counter = AtomicInteger()

            val flow = counterBasedFlow(counter)
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
                unfoldParallel(maxParallelism = maxParallelism, increaseStrategy = ParallelismIncreaseStrategy.MaxAllowedAfterReceive) {
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
}) {
    fun counterBasedFlow(counter: AtomicInteger): Flow<Int> =
        unfold { listOf(counter.incrementAndGet()) }
}
