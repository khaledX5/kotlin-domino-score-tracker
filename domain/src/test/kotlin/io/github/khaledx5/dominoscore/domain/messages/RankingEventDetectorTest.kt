package io.github.khaledx5.dominoscore.domain.messages

import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.ALL_PLAYERS_NEGATIVE
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.ESCAPED_KOOZ
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.EXCEEDED_TARGET
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.EXTREMELY_NEGATIVE_SCORE
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.FIRST_TIME_KING
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.FIRST_TIME_KOOZ
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.FIRST_TO_LAST
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.GAINED_THREE_RANKS
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.GENERAL_HUMOR
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.KING_COMEBACK
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.KOOZ_COMEBACK
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.LAST_TO_FIRST
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.LOST_LONG_STANDING_FIRST
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.LOST_TWO_RANKS
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.MAJOR_COMEBACK
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.NEGATIVE_TO_POSITIVE
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.ONE_POINT_LOST_FIRST
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.OVERTAKEN_PLAYER
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.REACHED_TARGET
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.STILL_KING
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.STILL_KOOZ
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.TIED_FIRST
import io.github.khaledx5.dominoscore.domain.messages.MessageTrigger.ROUND_END
import io.github.khaledx5.dominoscore.domain.messages.MessageTrigger.SCORE_CHANGE
import io.github.khaledx5.dominoscore.domain.messages.MessageTrigger.TARGET_CHANGE
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.scores
import io.github.khaledx5.dominoscore.domain.target
import org.junit.Assert.assertEquals
import org.junit.Test

/** Seats are player ids here: seat 0 is PlayerId(0). Results list one category (or null) per seat. */
class RankingEventDetectorTest {

    private fun detect(
        before: List<Int>,
        after: List<Int>,
        trigger: MessageTrigger = SCORE_CHANGE,
        actor: Int? = null,
        targetBefore: Int = 100,
        targetAfter: Int = targetBefore,
        history: Map<Int, TitleHistory> = emptyMap(),
    ): List<MessageCategory?> {
        val events = RankingEventDetector.detect(
            TableChange(
                before = scores(*before.toIntArray()),
                after = scores(*after.toIntArray()),
                targetBefore = target(targetBefore),
                targetAfter = target(targetAfter),
                trigger = trigger,
                actor = actor?.let { PlayerId(it.toLong()) },
            ),
            history.mapKeys { PlayerId(it.key.toLong()) },
        ).associate { it.playerId.value.toInt() to it.category }
        return after.indices.map { events[it] }
    }

    // ---- KING and KOOZ transitions ----

    @Test
    fun theFirstScoreOfTheGameMakesAFirstKingAndFirstKoozs() {
        assertEquals(
            listOf(FIRST_TIME_KING, FIRST_TIME_KOOZ, FIRST_TIME_KOOZ),
            detect(before = listOf(0, 0, 0), after = listOf(10, 0, 0)),
        )
    }

    @Test
    fun takingTheTopAgainIsAKingComeback() {
        val result = detect(
            before = listOf(25, 30, 0, 20),
            after = listOf(35, 30, 0, 20),
            history = mapOf(0 to TitleHistory(everKing = true)),
        )
        assertEquals(KING_COMEBACK, result[0])
    }

    @Test
    fun fallingToTheBottomAgainIsAKoozComeback() {
        val result = detect(
            before = listOf(40, 30, 10, 20),
            after = listOf(40, 30, 10, 5),
            history = mapOf(3 to TitleHistory(everKooz = true)),
        )
        assertEquals(KOOZ_COMEBACK, result[3])
    }

    @Test
    fun aScoreTapThatChangesNoStandingSaysNothing() {
        assertEquals(listOf(null, null, null), detect(before = listOf(50, 30, 10), after = listOf(55, 30, 10)))
    }

    // ---- round end ----

    @Test
    fun aRoundEndGivesStillKingStillKoozAndGeneralHumor() {
        assertEquals(
            listOf(STILL_KING, GENERAL_HUMOR, STILL_KOOZ),
            detect(before = listOf(50, 30, 10), after = listOf(50, 30, 10), trigger = ROUND_END),
        )
    }

    @Test
    fun aRoundEndWithATransitionDoesNotAlsoSayStill() {
        val result = detect(before = listOf(50, 30, 10), after = listOf(50, 60, 10), trigger = ROUND_END)
        assertEquals(listOf(OVERTAKEN_PLAYER, FIRST_TIME_KING, STILL_KOOZ), result)
    }

    @Test
    fun aRoundEndOnAFullTieIsGeneralHumorForEveryone() {
        assertEquals(
            listOf(GENERAL_HUMOR, GENERAL_HUMOR),
            detect(before = listOf(0, 0), after = listOf(0, 0), trigger = ROUND_END),
        )
    }

    // ---- special events ----

    @Test
    fun lastToFirstBeatsEveryOtherReason() {
        // Also a new KING, escaped KOOZ, negative to positive and three ranks gained: LAST_TO_FIRST wins.
        val result = detect(before = listOf(30, 20, 10, -5), after = listOf(30, 20, 10, 40))
        assertEquals(LAST_TO_FIRST, result[3])
    }

    @Test
    fun lastToFirstNeedsThreePlayers() {
        // With two players every change of leader is just one player passing the other.
        assertEquals(listOf(OVERTAKEN_PLAYER, FIRST_TIME_KING), detect(before = listOf(20, 10), after = listOf(20, 30)))
    }

    @Test
    fun firstToLast() {
        assertEquals(FIRST_TO_LAST, detect(before = listOf(50, 30, 20), after = listOf(10, 30, 20))[0])
    }

    @Test
    fun aKoozAtAnEarlierRoundEndTakingTheTopIsAMajorComeback() {
        val result = detect(
            before = listOf(50, 45, 40),
            after = listOf(50, 60, 40),
            history = mapOf(1 to TitleHistory(everKooz = true, koozAtARoundEnd = true)),
        )
        assertEquals(MAJOR_COMEBACK, result[1])
    }

    @Test
    fun losingAKingHeldForThreeRoundsIsTheEndOfAnEra() {
        val result = detect(
            before = listOf(50, 45, 10),
            after = listOf(50, 45, 60),
            history = mapOf(0 to TitleHistory(everKing = true, kingStreakRounds = 3)),
        )
        assertEquals(LOST_LONG_STANDING_FIRST, result[0])
    }

    @Test
    fun losingFirstPlaceByExactlyOnePoint() {
        assertEquals(ONE_POINT_LOST_FIRST, detect(before = listOf(50, 45, 10), after = listOf(50, 51, 10))[0])
    }

    @Test
    fun losingFirstPlaceByMoreIsJustBeingOvertaken() {
        assertEquals(OVERTAKEN_PLAYER, detect(before = listOf(50, 45, 10), after = listOf(50, 70, 10))[0])
    }

    @Test
    fun reachingTheTarget() {
        assertEquals(REACHED_TARGET, detect(before = listOf(90, 95, 10), after = listOf(90, 105, 10))[1])
    }

    @Test
    fun reachingTheTargetWithAQuarterToSpareIsExceedingIt() {
        assertEquals(EXCEEDED_TARGET, detect(before = listOf(90, 95, 10), after = listOf(90, 125, 10))[1])
    }

    @Test
    fun loweringTheTargetCanMakeSomeoneReachIt() {
        val result = detect(
            before = listOf(60, 30, 10),
            after = listOf(60, 30, 10),
            trigger = TARGET_CHANGE,
            targetBefore = 100,
            targetAfter = 50,
        )
        assertEquals(listOf(REACHED_TARGET, null, null), result)
    }

    @Test
    fun climbingOutOfLastPlaceIsEscapingKooz() {
        assertEquals(ESCAPED_KOOZ, detect(before = listOf(50, 30, 10), after = listOf(50, 30, 40))[2])
    }

    @Test
    fun goingFromNegativeToPositive() {
        assertEquals(NEGATIVE_TO_POSITIVE, detect(before = listOf(50, -10, -20), after = listOf(50, 5, -20))[1])
    }

    @Test
    fun climbingTwoRanksAtOnceIsTheRocket() {
        assertEquals(GAINED_THREE_RANKS, detect(before = listOf(60, 40, 30, 20), after = listOf(60, 40, 70, 20))[2])
    }

    @Test
    fun lastToSecondIsEscapingKoozNotTheRocket() {
        assertEquals(ESCAPED_KOOZ, detect(before = listOf(60, 40, 30, 20), after = listOf(60, 40, 30, 50))[3])
    }

    @Test
    fun droppingBelowSomeoneWhoDidNotMoveIsNotBeingOvertaken() {
        assertEquals(FIRST_TIME_KOOZ, detect(before = listOf(40, 30, 10, 20), after = listOf(40, 30, 10, 5))[3])
    }

    @Test
    fun droppingTwoRanksAtOnce() {
        assertEquals(LOST_TWO_RANKS, detect(before = listOf(60, 40, 30, 20), after = listOf(60, 15, 30, 20))[1])
    }

    @Test
    fun catchingTheLeaderIsATieForFirst() {
        assertEquals(
            listOf(TIED_FIRST, TIED_FIRST, null),
            detect(before = listOf(50, 40, 10), after = listOf(50, 50, 10)),
        )
    }

    @Test
    fun beingPassedBySomeoneWhoWasBehind() {
        assertEquals(OVERTAKEN_PLAYER, detect(before = listOf(60, 40, 30, 20), after = listOf(60, 40, 45, 20))[1])
    }

    @Test
    fun sinkingBelowMinusHalfTheTarget() {
        assertEquals(EXTREMELY_NEGATIVE_SCORE, detect(before = listOf(50, 30, -40), after = listOf(50, 30, -55))[2])
    }

    @Test
    fun theTapThatMakesEveryoneNegativeGetsTheDebtJoke() {
        val result = detect(before = listOf(-10, 5, -20), after = listOf(-10, -1, -20), actor = 1)
        assertEquals(ALL_PLAYERS_NEGATIVE, result[1])
    }

    @Test
    fun specialEventsAreOnlyGivenWhenTheyReallyHappen() {
        // A small step in the middle of the table: no rank, title, target or sign changes.
        assertEquals(
            listOf(null, null, null, null),
            detect(before = listOf(60, 40, 30, 20), after = listOf(60, 38, 30, 20)),
        )
    }

    @Test
    fun negativeScoresRankLikeAnyOther() {
        assertEquals(
            listOf(FIRST_TIME_KOOZ, FIRST_TIME_KING),
            detect(before = listOf(0, 0), after = listOf(-15, 0)),
        )
    }
}
