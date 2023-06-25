package com.river.core

@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This operation is currently being evaluated and is in preview state. " +
        "Be aware that it may possibly change in the future, resulting in a breaking change. " +
        "You should use it with '@com.river.core.RiverPreview' or '@OptIn(com.river.core.RiverPreview::class)'"
)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS, AnnotationTarget.PROPERTY)
annotation class RiverPreview
