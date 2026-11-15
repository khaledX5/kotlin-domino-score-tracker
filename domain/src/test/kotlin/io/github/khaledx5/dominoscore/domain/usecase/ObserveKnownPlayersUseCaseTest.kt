package io.github.khaledx5.dominoscore.domain.usecase

import app.cash.turbine.test
import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveKnownPlayersUseCaseTest {

    private val repository = FakeGameRepository()
    private val startGame = StartGameUseCase(repository, fixedClock)

    @Test
    fun listsPeopleFromTheLatestGameFirstOnceEach() = runTest {
        startGame(100, listOf("Omar", "Mona"))
        startGame(100, listOf("Ali", "omar"), replaceActiveGame = true)

        ObserveKnownPlayersUseCase(repository)().test {
            assertEquals(listOf("Omar", "Ali", "Mona"), awaitItem().map { it.name.value })
        }
    }

    @Test
    fun keepsOnlyTheMostRecentPeople() = runTest {
        startGame(100, listOf("Omar", "Mona", "Ali", "Sara"))

        ObserveKnownPlayersUseCase(repository)(limit = 2).test {
            assertEquals(listOf("Sara", "Ali"), awaitItem().map { it.name.value })
        }
    }

    @Test
    fun theSameNameKeepsOneProfile() = runTest {
        startGame(100, listOf("Omar", "Mona"))
        val first = repository.activeGame().players.first().id
        startGame(100, listOf("OMAR", "Ali"), replaceActiveGame = true)
        val second = repository.activeGame().players.first().id

        assertEquals(repository.profileOf(first), repository.profileOf(second))
    }
}
