package io.github.khaledx5.dominoscore.domain.model

import io.github.khaledx5.dominoscore.domain.game
import io.github.khaledx5.dominoscore.domain.player
import io.github.khaledx5.dominoscore.domain.scores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class GameTest {

    @Test
    fun acceptsTwoToFourPlayers() {
        assertEquals(2, game(scores(0, 0)).players.size)
        assertEquals(4, game(scores(0, 0, 0, 0)).players.size)
    }

    @Test
    fun rejectsTooFewOrTooManyPlayers() {
        assertThrows(IllegalArgumentException::class.java) { game(scores(0)) }
        assertThrows(IllegalArgumentException::class.java) { game(scores(0, 0, 0, 0, 0)) }
    }

    @Test
    fun rejectsTwoPlayersOnTheSameSeat() {
        assertThrows(IllegalArgumentException::class.java) { game(listOf(player(1), player(1, name = "Other"))) }
    }

    @Test
    fun findsPlayersById() {
        val game = game(scores(10, 20))

        assertEquals(20, game.player(PlayerId(1))?.score)
        assertNull(game.player(PlayerId(9)))
    }
}
