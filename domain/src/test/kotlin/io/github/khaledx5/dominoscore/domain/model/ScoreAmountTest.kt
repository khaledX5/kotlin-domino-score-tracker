package io.github.khaledx5.dominoscore.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreAmountTest {

    @Test
    fun acceptsBothEndsOfTheAllowedRange() {
        assertEquals(1, (ScoreAmount.of(1) as Outcome.Success).value.value)
        assertEquals(9_999, (ScoreAmount.of(9_999) as Outcome.Success).value.value)
    }

    @Test
    fun rejectsZeroBecauseTheTypeAlreadyCarriesTheSign() {
        assertEquals(Outcome.Failure(RangeError.TOO_LOW), ScoreAmount.of(0))
        assertEquals(Outcome.Failure(RangeError.TOO_LOW), ScoreAmount.of(-5))
    }

    @Test
    fun rejectsAmountsAboveTheMaximum() {
        assertEquals(Outcome.Failure(RangeError.TOO_HIGH), ScoreAmount.of(10_000))
    }
}
