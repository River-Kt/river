package com.river.core

data class ConcurrencyInfo(
    val maximum: Int,
    val current: Int
) {
   init {
       require(maximum >= 1 && current >= 1) {
           "both maximum and current must be >= 1"
       }

       require(maximum >= current) {
           "maximum must be >= current"
       }
   }
}
