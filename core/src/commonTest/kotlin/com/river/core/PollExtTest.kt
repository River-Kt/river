@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalRiverApi::class)

package com.river.core

import com.river.core.ConcurrencyStrategy.Companion.increaseByOne
import com.river.core.ConcurrencyStrategy.Companion.maximumAllowedAfterFirstIteration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.mpp.atomics.AtomicReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

class PollExtTest : FunSpec({
    test("building a simple non-stopping flow") {
        val counter = AtomicReference(0)
        val count = 100

        val flow = counterBasedFlow(counter)
        val list = flow.take(count).toList()

        list shouldContainInOrder (1..count).toList()
        counter.value shouldBe count
    }

    test("building a parallelized non-stopping flow") {
        val count = 1000
        val counter = AtomicReference(0)

        val flow = poll(increaseByOne(5)) { listOf(counter.increment()) }
        val list = flow.take(count).toList()

        list shouldContainInOrder (1..count).toList()
        counter.value shouldBe count
    }

    test("building a parallelized repeat flow that stops on empty lists") {
        val count = 1000
        val counter = AtomicReference(0)

        val flow = poll(increaseByOne(5), stopOnEmptyList = true) {
            if (counter.value == count) emptyList()
            else listOf(counter.increment())
        }

        val list = flow.toList()

        list shouldContainInOrder (1..count).toList()
        counter.value shouldBe count
    }

    test("asserting the increase strategy is working properly") {
        val counter = AtomicReference(0)
        val maxConcurrency = 10

        val gap = ((100..120) + (250..270) + (850..980))
        val changedConcurrency = AtomicReference(1 to 0)

        val flow =
            poll(maximumAllowedAfterFirstIteration(maxConcurrency)) {
                val c = counter.increment()

                val (last, count) = changedConcurrency.value

                if (last != current) {
                    changedConcurrency.value = current to count + 1
                }

                if (c in gap) emptyList()
                else listOf(c)
            }

        val list = flow.earlyCompleteIf { it > 1000 }.toList()

        val expected = ((1..1000) - gap).toList()
        list shouldBe expected
        changedConcurrency.value shouldBe (10 to 7)
    }

    test("building an arithmetic series flow") {
        val count = 100

        val flow = pollWithState(1, { it > count }) { state ->
            (state + 1) to (0..state).toList()
        }

        flow.sum() shouldBe 171700
    }
})

fun AtomicReference<Int>.increment(): Int {
    val new = value + 1
    check(compareAndSet(value, new))
    return value
}

fun counterBasedFlow(counter: AtomicReference<Int>): Flow<Int> =
    poll { listOf(counter.increment()) }