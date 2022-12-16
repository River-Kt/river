package io.river.core

import io.river.core.internal.UnfoldFlow
import kotlinx.coroutines.flow.Flow

/**
 * Unfold elements into a Flow.
 *
 * @param stopOnEmptyList tells if the flow must complete on an empty list
 * @param unfolder function for the unfolding process
 *
 * @sample io.river.core.UnfoldSamples.infiniteIntFlow
 * @sample io.river.core.UnfoldSamples.queuePollingFlow
 */
fun <T> unfold(
    stopOnEmptyList: Boolean = false,
    unfolder: suspend ParallelismInfo.() -> List<T>
): Flow<T> =
    unfoldParallel(
        maxParallelism = 1,
        stopOnEmptyList = stopOnEmptyList,
        unfolder = unfolder
    )

fun <T> unfoldParallel(
    maxParallelism: Int,
    stopOnEmptyList: Boolean = false,
    minimumParallelism: Int = 1,
    increaseStrategy: ParallelismIncreaseStrategy = ParallelismIncreaseStrategy.ByOne,
    unfolder: suspend ParallelismInfo.() -> List<T>
): Flow<T> =
    UnfoldFlow(
        minimumParallelism = minimumParallelism,
        maxParallelism = maxParallelism,
        stopOnEmptyList = stopOnEmptyList,
        increaseStrategy = increaseStrategy,
        producer = unfolder
    )
