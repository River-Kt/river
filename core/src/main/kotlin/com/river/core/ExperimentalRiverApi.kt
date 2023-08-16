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
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This operation is being evaluated and may not exist in the future. " +
            "You can provide feedback about the operation directly via GitHub issues. " +
            "By doing so, the more likely the operation will be improved or marked as stable."
)
annotation class ExperimentalRiverApi
