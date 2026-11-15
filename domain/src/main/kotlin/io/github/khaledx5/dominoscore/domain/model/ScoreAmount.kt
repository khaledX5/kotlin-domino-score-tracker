package io.github.khaledx5.dominoscore.domain.model

/** Points entered for one score operation. Always positive: the operation type decides the sign. */
@JvmInline
value class ScoreAmount private constructor(val value: Int) {

    companion object {
        val ALLOWED = 1..9_999

        fun of(value: Int): Outcome<ScoreAmount, RangeError> = when (val error = ALLOWED.check(value)) {
            null -> Outcome.Success(ScoreAmount(value))
            else -> Outcome.Failure(error)
        }
    }
}
