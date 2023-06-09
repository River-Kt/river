package com.river.core

import com.river.core.ConcurrencyStrategy.Companion.increaseByOne
import com.river.core.ConcurrencyStrategy.Companion.maximumAllowedAfterFirstIteration
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class PollExtTest : FeatureSpec({
    this as PollExtTest

    feature("poll: Flow<T>") {
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

            val flow = poll(increaseByOne(5)) { listOf(counter.incrementAndGet()) }
            val list = flow.take(count).toList()

            list shouldContainInOrder (1..count).toList()
            counter.get() shouldBe count
        }

        scenario("building a parallelized repeat flow that stops on empty lists") {
            val count = 1000
            val counter = AtomicInteger()

            val flow = poll(increaseByOne(5), stopOnEmptyList = true) {
                if (counter.get() == count) emptyList()
                else listOf(counter.incrementAndGet())
            }

            val list = flow.toList()

            list shouldContainInOrder (1..count).toList()
            counter.get() shouldBe count
        }

        scenario("asserting the increase strategy is working properly") {
            val counter = AtomicInteger()
            val maxConcurrency = 10

            val gap = ((100..120) + (250..270) + (850..980))
            val changedConcurrency = AtomicReference(1 to 0)

            val flow =
                poll(maximumAllowedAfterFirstIteration(maxConcurrency)) {
                    val c = counter.incrementAndGet()

                    val (last, count) = changedConcurrency.get()

                    if (last != current) {
                        changedConcurrency.set(current to count + 1)
                    }

                    if (c in gap) emptyList()
                    else listOf(c)
                }

            val list = flow.earlyCompleteIf { it > 1000 }.toList()

            val expected = ((1..1000) - gap).toList()
            list shouldBe expected
            changedConcurrency.get() shouldBe (10 to 7)
        }
    }

    feature("pollWithState: Flow<T>") {
        scenario("building an arithmetic series flow") {
            val count = 100

            val flow = pollWithState(1, { it > count }) { state ->
                (state + 1) to (0..state).toList()
            }

            flow.sum() shouldBe 171700
        }
    }
}) {
    fun counterBasedFlow(counter: AtomicInteger): Flow<Int> =
        poll { listOf(counter.incrementAndGet()) }
}
