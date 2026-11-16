package io.github.khaledx5.dominoscore.domain.sound

import io.github.khaledx5.dominoscore.domain.command.GameCommand
import io.github.khaledx5.dominoscore.domain.command.GameTransition
import io.github.khaledx5.dominoscore.domain.game
import io.github.khaledx5.dominoscore.domain.messages.ScoreLog
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType.ADD
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType.PENALTY
import io.github.khaledx5.dominoscore.domain.scores
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent.EQUAL_SCORES
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent.FINAL_STRETCH
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent.LARGE_PENALTY
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent.LAST_PLAYER_SCORES
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent.SECOND_QUALIFIED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameSoundEventDetectorTest {

    private val detector = GameSoundEventDetector()

    /** Scores [seat] by [points] (negative = penalty) on a table of [before] scores. */
    private fun score(before: List<Int>, seat: Int, points: Int, target: Int = 150): GameSoundEvent? {
        val after = before.toMutableList().also { it[seat] += points }
        val type = if (points >= 0) ADD else PENALTY
        val command = GameCommand.Score(PlayerId(seat.toLong()), type, kotlin.math.abs(points))
        return detector.detect(transition(command, table(before, target), table(after, target)))
    }

    private fun changeTarget(scores: List<Int>, from: Int, to: Int): GameSoundEvent? =
        detector.detect(transition(GameCommand.ChangeTarget, table(scores, from), table(scores, to)))

    private fun table(values: List<Int>, target: Int): Game = game(scores(*values.toIntArray()), target)

    private fun transition(command: GameCommand, before: Game, after: Game) =
        GameTransition(command, before, ScoreLog(), after)

    // Large penalty: the operation itself, never a sum.

    @Test
    fun aPenaltyOfFourIsNotLarge() = assertNull(score(listOf(10, 10), seat = 0, points = -4))

    @Test
    fun aPenaltyOfFiveOrMoreIsLarge() {
        assertEquals(LARGE_PENALTY, score(listOf(10, 3), seat = 0, points = -5))
        assertEquals(LARGE_PENALTY, score(listOf(10, 3), seat = 0, points = -10))
    }

    @Test
    fun smallPenaltiesNeverAddUpToALargeOne() {
        assertNull(score(listOf(20, 3), seat = 0, points = -3))
        assertNull(score(listOf(17, 3), seat = 0, points = -3))
    }

    @Test
    fun aBigAddIsNotAPenalty() = assertNull(score(listOf(10, 3), seat = 0, points = 10))

    // Equal scores: a new group above 25.

    @Test
    fun aTieAtTwentyFiveDoesNotCount() = assertNull(score(listOf(20, 25), seat = 0, points = 5))

    @Test
    fun aTieAtTwentySixCounts() = assertEquals(EQUAL_SCORES, score(listOf(20, 26), seat = 0, points = 6))

    @Test
    fun aThreeWayTieCounts() = assertEquals(EQUAL_SCORES, score(listOf(30, 30, 20), seat = 2, points = 10))

    @Test
    fun aTieThatJustStaysDoesNotPlayAgain() = assertNull(score(listOf(30, 30, 3), seat = 2, points = 1))

    @Test
    fun aTieThatIsBrokenAndMadeAgainCountsAgain() {
        assertNull(score(listOf(30, 30), seat = 0, points = 2))
        assertEquals(EQUAL_SCORES, score(listOf(32, 30), seat = 1, points = 2))
    }

    // Final stretch: the top two both within the last 10 points, target 51 means 41..50.

    @Test
    fun fortyFiveAndFortyIsNotTheFinalStretch() = assertNull(score(listOf(45, 30), seat = 1, points = 10, target = 51))

    @Test
    fun fortyFiveAndFortyOneIsTheFinalStretch() =
        assertEquals(FINAL_STRETCH, score(listOf(45, 40), seat = 1, points = 1, target = 51))

    @Test
    fun reachingTheTargetIsNotTheStretch() = assertNull(score(listOf(40, 44), seat = 0, points = 11, target = 51))

    @Test
    fun aLowerTargetCanStartTheFinalStretch() = assertEquals(FINAL_STRETCH, changeTarget(listOf(45, 42), 100, 51))

    @Test
    fun theStretchPlaysOnceWhileItLasts() = assertNull(score(listOf(45, 42), seat = 1, points = 1, target = 51))

    // Last player scores: the only one on 0 gets off it.

    @Test
    fun theLastPlayerOnZeroScoringOnePointCounts() =
        assertEquals(LAST_PLAYER_SCORES, score(listOf(10, 5, 0), seat = 2, points = 1))

    @Test
    fun theLastPlayerOnZeroScoringFivePointsCounts() =
        assertEquals(LAST_PLAYER_SCORES, score(listOf(10, 5, 0), seat = 2, points = 5))

    @Test
    fun goingFromMinusFiveToMinusOneDoesNotCount() = assertNull(score(listOf(10, 5, -5), seat = 2, points = 4))

    @Test
    fun anotherPlayerStillOnZeroMeansItIsNotTheLast() = assertNull(score(listOf(10, 0, 0), seat = 2, points = 3))

    @Test
    fun othersBelowZeroMeanItIsNotTheLast() = assertNull(score(listOf(10, -3, 0), seat = 2, points = 3))

    // Second qualified: fewer than two qualified before, two or more after.

    @Test
    fun theFirstQualifiedPlayerIsNotTheSecond() = assertNull(score(listOf(90, 60), seat = 0, points = 10, target = 100))

    @Test
    fun theSecondQualifiedPlayerCounts() =
        assertEquals(SECOND_QUALIFIED, score(listOf(100, 60), seat = 1, points = 40, target = 100))

    @Test
    fun passingTheTargetAlsoCounts() =
        assertEquals(SECOND_QUALIFIED, score(listOf(120, 90), seat = 1, points = 40, target = 100))

    @Test
    fun aThirdQualifiedPlayerDoesNotCount() =
        assertNull(score(listOf(100, 110, 50), seat = 2, points = 55, target = 100))

    @Test
    fun losingAndRegainingTheSecondPlaceCountsAgain() {
        assertNull(score(listOf(100, 100), seat = 1, points = -3, target = 100))
        assertEquals(SECOND_QUALIFIED, score(listOf(100, 97), seat = 1, points = 3, target = 100))
    }

    @Test
    fun loweringTheTargetSoTwoQualifyAtOnceCounts() =
        assertEquals(SECOND_QUALIFIED, changeTarget(listOf(80, 85), from = 100, to = 80))

    // One command, one sound.

    @Test
    fun theHighestPriorityEventWins() {
        val before = table(listOf(100, 90), target = 100)
        val after = table(listOf(100, 100), target = 100)
        val transition = transition(GameCommand.Score(PlayerId(1), ADD, 10), before, after)

        assertEquals(setOf(SECOND_QUALIFIED, EQUAL_SCORES), detector.detectAll(transition))
        assertEquals(SECOND_QUALIFIED, detector.detect(transition))
    }

    @Test
    fun endingARoundChangesNoScoreAndPlaysNothing() {
        val table = table(listOf(30, 30, 0), target = 100)
        assertNull(detector.detect(transition(GameCommand.EndRound, table, table)))
    }

    @Test
    fun thePriorityOrderIsTheAgreedOne() = assertEquals(
        listOf(SECOND_QUALIFIED, FINAL_STRETCH, LAST_PLAYER_SCORES, EQUAL_SCORES, LARGE_PENALTY),
        GameSoundEvent.entries.toList(),
    )
}
