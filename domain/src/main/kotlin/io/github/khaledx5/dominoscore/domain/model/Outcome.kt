package io.github.khaledx5.dominoscore.domain.model

/**
 * Result of a domain operation that can fail for an expected business reason.
 * Expected failures are values, so callers handle them explicitly instead of catching exceptions.
 */
sealed interface Outcome<out T, out E> {
    data class Success<out T>(val value: T) : Outcome<T, Nothing>

    data class Failure<out E>(val error: E) : Outcome<Nothing, E>
}

inline fun <T, E, R> Outcome<T, E>.map(transform: (T) -> R): Outcome<R, E> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T, E, F> Outcome<T, E>.mapError(transform: (E) -> F): Outcome<T, F> = when (this) {
    is Outcome.Success -> this
    is Outcome.Failure -> Outcome.Failure(transform(error))
}

fun <T> Outcome<T, *>.getOrNull(): T? = (this as? Outcome.Success)?.value
