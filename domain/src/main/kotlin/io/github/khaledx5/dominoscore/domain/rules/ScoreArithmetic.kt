package io.github.khaledx5.dominoscore.domain.rules

import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.ScoreAmount
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType

/** The new score would not fit in an [Int]. Rejected instead of silently wrapping around. */
data object ScoreOutOfRange

/**
 * Applies one operation to a cumulative score. Scores are signed and have no floor,
 * so a penalty can take a player below zero.
 */
fun ScoreOperationType.applyTo(currentScore: Int, amount: ScoreAmount): Outcome<Int, ScoreOutOfRange> = try {
    val newScore = when (this) {
        ScoreOperationType.ADD -> Math.addExact(currentScore, amount.value)
        ScoreOperationType.PENALTY -> Math.subtractExact(currentScore, amount.value)
    }
    Outcome.Success(newScore)
} catch (_: ArithmeticException) {
    Outcome.Failure(ScoreOutOfRange)
}
