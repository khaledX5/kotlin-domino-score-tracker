package io.github.khaledx5.dominoscore.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TargetScoreTest {

    @Test
    fun acceptsBothEndsOfTheAllowedRange() {
        assertEquals(1, (TargetScore.of(1) as Outcome.Success).value.value)
        assertEquals(99_999, (TargetScore.of(99_999) as Outcome.Success).value.value)
    }

    @Test
    fun rejectsZeroAndNegativeTargets() {
        assertEquals(Outcome.Failure(RangeError.TOO_LOW), TargetScore.of(0))
        assertEquals(Outcome.Failure(RangeError.TOO_LOW), TargetScore.of(-150))
    }

    @Test
    fun rejectsTargetsAboveTheMaximum() {
        assertEquals(Outcome.Failure(RangeError.TOO_HIGH), TargetScore.of(100_000))
    }
}
