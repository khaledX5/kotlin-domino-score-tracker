package io.github.khaledx5.dominoscore.domain.model

/** The score a player has to reach to qualify for the win. */
@JvmInline
value class TargetScore private constructor(val value: Int) {

    companion object {
        val ALLOWED = 1..99_999

        fun of(value: Int): Outcome<TargetScore, RangeError> = when (val error = ALLOWED.check(value)) {
            null -> Outcome.Success(TargetScore(value))
            else -> Outcome.Failure(error)
        }
    }
}
