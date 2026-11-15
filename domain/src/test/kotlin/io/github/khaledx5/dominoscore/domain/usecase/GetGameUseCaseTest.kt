package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.valueOrFail
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetGameUseCaseTest {

    private val games = FakeGameRepository()
    private val getGame = GetGameUseCase(games)

    @Test
    fun findsAGameThatIsNoLongerActive() = runTest {
        val first = StartGameUseCase(games, fixedClock)(100, listOf("Omar", "Mona")).valueOrFail()
        StartGameUseCase(games, fixedClock)(100, listOf("Ali", "Sara"), replaceActiveGame = true)

        val snapshot = getGame(first)!!

        assertEquals(GameStatus.ABANDONED, snapshot.game.status)
        assertEquals(listOf("Omar", "Mona"), snapshot.ranking.map { it.player.name.value })
    }

    @Test
    fun anUnknownIdIsNull() = runTest {
        assertNull(getGame(GameId(99)))
    }
}
