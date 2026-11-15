package io.github.khaledx5.dominoscore.domain.messages

import io.github.khaledx5.dominoscore.domain.T0
import io.github.khaledx5.dominoscore.domain.amount
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.Round
import io.github.khaledx5.dominoscore.domain.model.RoundId
import io.github.khaledx5.dominoscore.domain.model.RoundStatus
import io.github.khaledx5.dominoscore.domain.model.ScoreOperation
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.scores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleTimelineTest {

    private val players = scores(0, 0, 0)

    /** A round of the log: each pair is (seat, new score), applied in order. */
    private class LogBuilder {
        val rounds = mutableListOf<Round>()
        val operations = mutableListOf<ScoreOperation>()
        private val current = mutableMapOf<Int, Int>()

        fun round(number: Int, completed: Boolean, vararg moves: Pair<Int, Int>) {
            val roundId = RoundId(number.toLong())
            moves.forEach { (seat, newScore) ->
                val before = current[seat] ?: 0
                operations += ScoreOperation(
                    roundId = roundId,
                    playerId = PlayerId(seat.toLong()),
                    type = if (newScore >= before) ScoreOperationType.ADD else ScoreOperationType.PENALTY,
                    amount = amount(maxOf(1, kotlin.math.abs(newScore - before))),
                    previousScore = before,
                    newScore = newScore,
                    createdAt = T0,
                )
                current[seat] = newScore
            }
            if (completed) rounds += Round(roundId, number, RoundStatus.COMPLETED, T0, completedAt = T0)
        }

        fun build() = ScoreLog(rounds, operations)
    }

    private fun timeline(build: LogBuilder.() -> Unit) = TitleTimeline(players, LogBuilder().apply(build).build())

    private fun TitleTimeline.now(seat: Int) = untilNow.getValue(PlayerId(seat.toLong()))

    @Test
    fun aNewGameHasNoHistoryAndEndsTheLastRoundOnZero() {
        val timeline = timeline {}

        assertEquals(TitleHistory(), timeline.now(0))
        assertEquals(listOf(0, 0, 0), timeline.lastRoundEnd.map { it.score })
    }

    @Test
    fun everyTitleHeldAfterAnyTapIsRemembered() {
        val timeline = timeline { round(1, completed = false, 0 to 10, 1 to 20) }

        // Tap 1 (10, 0, 0): seat 0 KING, seats 1 and 2 KOOZ. Tap 2 (10, 20, 0): seat 1 KING, seat 2 KOOZ.
        assertTrue(timeline.now(0).everKing)
        assertFalse(timeline.now(0).everKooz)
        assertTrue(timeline.now(1).everKing)
        assertTrue(timeline.now(1).everKooz)
        assertFalse(timeline.now(2).everKing)
    }

    @Test
    fun theKingStreakCountsCompletedRoundsInARow() {
        val timeline = timeline {
            round(1, completed = true, 0 to 10)
            round(2, completed = true, 1 to 5)
            round(3, completed = true, 2 to 3)
        }

        assertEquals(3, timeline.now(0).kingStreakRounds)
        assertEquals(0, timeline.now(1).kingStreakRounds)
    }

    @Test
    fun losingTheTopAtARoundEndResetsTheStreak() {
        val timeline = timeline {
            round(1, completed = true, 0 to 10)
            round(2, completed = true, 1 to 30)
        }

        assertEquals(0, timeline.now(0).kingStreakRounds)
        assertEquals(1, timeline.now(1).kingStreakRounds)
    }

    @Test
    fun koozAtARoundEndIsRemembered() {
        val timeline = timeline {
            round(1, completed = true, 0 to 10, 1 to 5)
            round(2, completed = false, 2 to 40)
        }

        assertTrue(timeline.now(2).koozAtARoundEnd)
        assertFalse(timeline.now(0).koozAtARoundEnd)
    }

    @Test
    fun theLastRoundEndIgnoresTheRoundInProgress() {
        val timeline = timeline {
            round(1, completed = true, 0 to 10)
            round(2, completed = false, 1 to 50)
        }

        assertEquals(listOf(10, 0, 0), timeline.lastRoundEnd.map { it.score })
        // Seat 1 only became KING in the round still in progress.
        assertFalse(timeline.untilLastRoundEnd.getValue(PlayerId(1)).everKing)
        assertTrue(timeline.now(1).everKing)
    }
}
