package io.github.gabfssilva.river.jms

sealed class JmsPrimitive<T>(val value: T) {
    class Text(value: String) : JmsPrimitive<String>(value)
    class Long(value: kotlin.Long) : JmsPrimitive<kotlin.Long>(value)
    class Int(value: kotlin.Int) : JmsPrimitive<kotlin.Int>(value)
    class Boolean(value: kotlin.Boolean) : JmsPrimitive<kotlin.Boolean>(value)
    class Bytes(value: ByteArray) : JmsPrimitive<ByteArray>(value)
    class Double(value: kotlin.Double) : JmsPrimitive<kotlin.Double>(value)
    class Float(value: kotlin.Float) : JmsPrimitive<kotlin.Float>(value)
}
