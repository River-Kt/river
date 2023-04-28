package com.river.core

data class ParallelismInfo(
    val maxAllowedParallelism: Int,
    val currentParallelism: Int
) {
   init {
       require(maxAllowedParallelism >= 1 && currentParallelism >= 1) {
           "both maxAllowedParallelism and currentParallelism must be >= 1"
       }

       require(maxAllowedParallelism >= currentParallelism) {
           "maxAllowedParallelism must be >= currentParallelism"
       }
   }
}
