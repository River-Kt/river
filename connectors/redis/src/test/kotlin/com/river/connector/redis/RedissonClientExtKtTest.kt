@file:OptIn(ExperimentalTime::class)

package com.river.connector.redis

import com.river.core.mapAsync
import com.river.core.zipAsync
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.comparables.shouldNotBeGreaterThan
import io.kotest.matchers.comparables.shouldNotBeLessThan
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import org.redisson.Redisson
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class RedissonClientExtKtTest : FeatureSpec({
    val redisson by lazy { Redisson.create() }

    feature("Redis semaphore") {
        scenario("Simple usage") {
            val semaphore = redisson.semaphore(name = "test-semaphore", concurrencyLevel = 50)

            val (_, duration) = measureTimedValue {
                (1..100)
                    .asFlow()
                    .mapAsync(semaphore) {
                        delay(500.milliseconds)
                        "Processing event $it"
                    }
                    .collect()
            }

            duration shouldNotBeLessThan (1).seconds
            duration shouldNotBeGreaterThan (1.2).seconds
        }

        scenario("Multiple flows sharing the same semaphore") {
            val semaphore = redisson.semaphore(name = "test-semaphore-3", concurrencyLevel = 50)

            suspend fun flowCollection() =
                (1..100)
                    .asFlow()
                    .mapAsync(semaphore) {
                        delay(500.milliseconds)
                        "Processing event $it"
                    }
                    .collect()

            val (_, duration) = measureTimedValue {
                zipAsync(
                    ::flowCollection,
                    ::flowCollection,
                    ::flowCollection,
                    ::flowCollection,
                    ::flowCollection
                ) { _, _, _, _, _ -> }
            }

            duration shouldNotBeLessThan (5.0).seconds
            duration shouldNotBeGreaterThan (5.3).seconds
        }
    }
})
