package com.river.connector.format.positional.flat.line

import com.river.core.ExperimentalRiverApi
import com.river.core.lines
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

@ExperimentalRiverApi
data class Field<T>(
    val name: String,
    val padding: Char,
    val range: IntRange,
    val parser: (String) -> T
) {
    fun parse(line: String): T =
        line
            .substring(range.first, range.last)
            .trim(padding)
            .let(parser)
}

@ExperimentalRiverApi
data class RecordDef(
    val fields: Map<String, Field<*>>
) {
    fun parse(line: String): Map<String, Any?> =
        fields.mapValues { (_, v) -> v.parse(line) }
}

@ExperimentalRiverApi
class RecordBuilder {
    val long: (String) -> Long = { it.toLong() }
    val int: (String) -> Int = { it.toInt() }
    val localDate: (String) -> LocalDate = { LocalDate.parse(it) }
    val boolean: (String) -> Boolean = { it.toBooleanStrict() }
    val string: (String) -> String = { it }

    private val fields: MutableList<Field<*>> = mutableListOf()

    var padding = ' '

    fun <T> field(field: Field<T>) {
        fields.add(field)
    }

    infix fun <F, T> Field<F>.mappedAs(
        p: (F) -> T
    ): Field<T> =
        Field(
            name = name,
            range = range,
            padding = padding,
            parser = { p(parser(it)) }
        )

    infix fun String.from(
        range: IntRange
    ): Field<String> = Field(this, padding, range, string)

    fun build() = RecordDef(fields.associateBy { it.name })
}

@ExperimentalRiverApi
fun record(builder: RecordBuilder.() -> Unit) =
    RecordBuilder()
        .also(builder)
        .build()

@ExperimentalRiverApi
infix fun <T> Map<String, Any?>.mappedAs(f: (Map<String, Any?>) -> T) =
    f(this)

@ExperimentalRiverApi
infix fun String.parsedTo(record: RecordDef) =
    record.parse(this)

@ExperimentalRiverApi
fun Flow<String>.toPositionalFlatLine(
    record: RecordBuilder.() -> Unit
): Flow<Map<String, Any?>> = toPositionalFlatLine(record(record))

@ExperimentalRiverApi
fun <T> Flow<String>.toPositionalFlatLine(
    mappedAs: (Map<String, Any?>) -> T,
    record: RecordBuilder.() -> Unit
): Flow<T> =
    toPositionalFlatLine(record(record))
        .map(mappedAs)

@ExperimentalRiverApi
fun Flow<String>.toPositionalFlatLine(
    record: RecordDef
): Flow<Map<String, Any?>> =
    lines().map { record.parse(it) }

@ExperimentalRiverApi
fun <T> Flow<String>.toPositionalFlatLine(
    record: RecordDef,
    f: (Map<String, Any?>) -> T
): Flow<T> =
    toPositionalFlatLine(record)
        .map(f)
