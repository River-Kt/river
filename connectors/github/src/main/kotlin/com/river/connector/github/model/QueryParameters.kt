package com.river.connector.github.model

import com.river.connector.github.internal.snakeCase
import kotlin.reflect.full.declaredMemberProperties

interface QueryParameters

internal fun QueryParameters.asMap(): Map<String, List<String>> =
    this::class.declaredMemberProperties
        .map { it.name to it.call(this) }
        .filter { (_, v) -> v != null }
        .toMap()
        .mapKeys { (k, _) -> k.snakeCase() }
        .mapValues { (_, value) ->
            when (value) {
                is List<*> -> value.map {
                    when (it) {
                        is Enum<*> -> it.name.lowercase()
                        else -> "$it"
                    }
                }

                is Enum<*> -> listOf(value.name.lowercase())

                else -> listOf("$value")
            }
        }
        .filter { (_, values) -> values.isNotEmpty() }
