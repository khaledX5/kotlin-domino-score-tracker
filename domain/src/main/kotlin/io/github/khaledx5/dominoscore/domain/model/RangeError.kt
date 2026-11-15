package io.github.khaledx5.dominoscore.domain.model

/** Why a whole number was rejected by a bounded value type. */
enum class RangeError {
    TOO_LOW,
    TOO_HIGH,
}

internal fun IntRange.check(value: Int): RangeError? = when {
    value < first -> RangeError.TOO_LOW
    value > last -> RangeError.TOO_HIGH
    else -> null
}
