package io.river.core

import kotlinx.coroutines.sync.Semaphore

class ConcurrencyInfo(
    private val totalSlots: Int,
    private val semaphore: Semaphore
) {
    val availableSlots: Int
        get() = semaphore.availablePermits

    val percentageOfAvailableSlots: Int
        get() = (availableSlots * 100) / totalSlots
}
