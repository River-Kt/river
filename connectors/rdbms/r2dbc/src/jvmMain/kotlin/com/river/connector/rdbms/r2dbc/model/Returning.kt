package com.river.connector.rdbms.r2dbc.model

sealed interface Returning {
    object Default : Returning
    data class GeneratedValues(val columns: List<String>) : Returning {
        companion object {
            operator fun invoke(vararg columns: String) = GeneratedValues(columns.toList())
        }
    }
}
