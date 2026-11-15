package io.github.khaledx5.dominoscore.domain.rules

import io.github.khaledx5.dominoscore.domain.player
import io.github.khaledx5.dominoscore.domain.scores
import org.junit.Assert.assertEquals
import org.junit.Test

class RankingTest {

    private fun ranksOf(vararg values: Int) = rankPlayers(scores(*values)).map { it.rank }

    private fun scoresInOrder(vararg values: Int) = rankPlayers(scores(*values)).map { it.player.score }

    @Test
    fun ordersByScoreHighestFirst() {
        assertEquals(listOf(90, 40, 10), scoresInOrder(10, 90, 40))
        assertEquals(listOf(1, 2, 3), ranksOf(10, 90, 40))
    }

    @Test
    fun equalScoresShareARankAndTheNextRankSkips() {
        assertEquals(listOf(1, 2, 2, 4), ranksOf(100, 150, 50, 100))
    }

    @Test
    fun aTieAtTheTopSharesFirstPlace() {
        assertEquals(listOf(1, 1, 3), ranksOf(120, 120, 30))
    }

    @Test
    fun everybodyIsFirstWhenAllScoresAreEqual() {
        assertEquals(listOf(1, 1, 1, 1), ranksOf(0, 0, 0, 0))
    }

    @Test
    fun negativeScoresRankBelowZero() {
        assertEquals(listOf(0, -10, -40), scoresInOrder(-40, 0, -10))
    }

    @Test
    fun equalScoresKeepSeatOrder() {
        val players = listOf(player(seat = 3, score = 50), player(seat = 1, score = 50), player(seat = 2, score = 80))

        assertEquals(listOf(2, 1, 3), rankPlayers(players).map { it.player.seat })
    }
}
