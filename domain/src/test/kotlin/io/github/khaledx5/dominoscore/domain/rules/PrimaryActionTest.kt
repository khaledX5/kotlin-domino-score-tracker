package io.github.khaledx5.dominoscore.domain.rules

import io.github.khaledx5.dominoscore.domain.player
import io.github.khaledx5.dominoscore.domain.scores
import io.github.khaledx5.dominoscore.domain.target
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimaryActionTest {

    private val target150 = target(150)

    @Test
    fun reachingTheTargetExactlyQualifies() {
        assertTrue(player(seat = 0, score = 150).isQualified(target150))
        assertFalse(player(seat = 0, score = 149).isQualified(target150))
    }

    @Test
    fun goingPastTheTargetQualifies() {
        assertTrue(player(seat = 0, score = 210).isQualified(target150))
    }

    @Test
    fun listsOnlyQualifiedPlayersInSeatOrder() {
        val qualified = qualifiedPlayers(scores(160, 20, 150, 149), target150)

        assertEquals(listOf(0, 2), qualified.map { it.seat })
    }

    @Test
    fun endRoundWhileNobodyIsQualified() {
        assertEquals(PrimaryAction.END_ROUND, primaryActionFor(scores(149, 0, -20), target150))
    }

    @Test
    fun endGameAsSoonAsOnePlayerIsQualified() {
        assertEquals(PrimaryAction.END_GAME, primaryActionFor(scores(149, 150), target150))
    }

    @Test
    fun aPenaltyBelowTheTargetBringsBackEndRound() {
        val afterPenalty = scores(140, 100)

        assertEquals(PrimaryAction.END_ROUND, primaryActionFor(afterPenalty, target150))
    }

    @Test
    fun raisingTheTargetBringsBackEndRound() {
        val players = scores(160, 100)

        assertEquals(PrimaryAction.END_GAME, primaryActionFor(players, target150))
        assertEquals(PrimaryAction.END_ROUND, primaryActionFor(players, target(200)))
    }

    @Test
    fun loweringTheTargetCanQualifySomeone() {
        assertEquals(PrimaryAction.END_GAME, primaryActionFor(scores(120, 100), target(100)))
    }
}
