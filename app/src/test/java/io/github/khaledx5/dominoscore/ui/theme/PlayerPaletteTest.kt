package io.github.khaledx5.dominoscore.ui.theme

import io.github.khaledx5.dominoscore.domain.model.PlayerColors
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerPaletteTest {

    @Test
    fun thereIsOneDistinctColorForEveryIndexTheDomainAllows() {
        val players = MidnightTealGameColors.players

        assertEquals(PlayerColors.COUNT, players.size)
        assertEquals(players.size, players.toSet().size)
    }
}
