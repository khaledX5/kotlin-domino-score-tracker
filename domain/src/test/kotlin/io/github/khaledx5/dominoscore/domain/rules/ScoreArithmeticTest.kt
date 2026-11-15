package io.github.khaledx5.dominoscore.domain.rules

import io.github.khaledx5.dominoscore.domain.amount
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType.ADD
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType.PENALTY
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreArithmeticTest {

    @Test
    fun addIncreasesTheScore() {
        assertEquals(Outcome.Success(75), ADD.applyTo(50, amount(25)))
    }

    @Test
    fun penaltyDecreasesTheScore() {
        assertEquals(Outcome.Success(30), PENALTY.applyTo(50, amount(20)))
    }

    @Test
    fun penaltyCanTakeTheScoreBelowZero() {
        assertEquals(Outcome.Success(-15), PENALTY.applyTo(10, amount(25)))
    }

    @Test
    fun addCanBringANegativeScoreBack() {
        assertEquals(Outcome.Success(5), ADD.applyTo(-15, amount(20)))
    }

    @Test
    fun rejectsAnAddThatWouldOverflow() {
        assertEquals(Outcome.Failure(ScoreOutOfRange), ADD.applyTo(Int.MAX_VALUE - 5, amount(6)))
    }

    @Test
    fun rejectsAPenaltyThatWouldUnderflow() {
        assertEquals(Outcome.Failure(ScoreOutOfRange), PENALTY.applyTo(Int.MIN_VALUE + 5, amount(6)))
    }

    @Test
    fun reachesTheIntLimitsExactly() {
        assertEquals(Outcome.Success(Int.MAX_VALUE), ADD.applyTo(Int.MAX_VALUE - 5, amount(5)))
        assertEquals(Outcome.Success(Int.MIN_VALUE), PENALTY.applyTo(Int.MIN_VALUE + 5, amount(5)))
    }
}
