package com.river.core

import kotlin.annotation.AnnotationTarget.*

@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@Target(
    CLASS,
    ANNOTATION_CLASS,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPEALIAS
)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalRiverApi
