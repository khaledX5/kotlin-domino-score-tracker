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

    private fun titlesOf(vararg values: Int) = rankPlayers(scores(*values)).map { ranked ->
        when {
            ranked.isKing && ranked.isKooz -> "KING+KOOZ"
            ranked.isKing -> "KING"
            ranked.isKooz -> "KOOZ"
            else -> "#${ranked.rank}"
        }
    }

    @Test
    fun firstPlaceIsKingAndLastPlaceIsKoozWithFourPlayers() {
        assertEquals(listOf("KING", "#2", "#3", "KOOZ"), titlesOf(40, 150, 90, 10))
    }

    @Test
    fun twoPlayersAreOneKingAndOneKooz() {
        assertEquals(listOf("KING", "KOOZ"), titlesOf(30, 75))
    }

    @Test
    fun everyPlayerTiedOnTheHighestScoreIsKing() {
        assertEquals(listOf("KING", "KING", "#3", "KOOZ"), titlesOf(120, 50, 120, 0))
    }

    @Test
    fun everyPlayerTiedOnTheLowestScoreIsKooz() {
        assertEquals(listOf("KING", "#2", "KOOZ", "KOOZ"), titlesOf(5, 200, 5, 60))
    }

    @Test
    fun whenAllScoresAreEqualEveryoneIsBothKingAndKooz() {
        assertEquals(listOf("KING+KOOZ", "KING+KOOZ", "KING+KOOZ"), titlesOf(0, 0, 0))
    }

    @Test
    fun negativeScoresCanBeKooz() {
        assertEquals(listOf("KING", "#2", "KOOZ"), titlesOf(-30, 0, 15))
    }

    @Test
    fun titlesFollowScoresNotSeats() {
        val players = listOf(player(seat = 0, score = 10), player(seat = 1, score = 90))

        val king = rankPlayers(players).single { it.isKing }
        val kooz = rankPlayers(players).single { it.isKooz }

        assertEquals(1, king.player.seat)
        assertEquals(0, kooz.player.seat)
    }
}
