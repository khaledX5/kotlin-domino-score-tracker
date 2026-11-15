package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.game
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerNameError
import io.github.khaledx5.dominoscore.domain.player
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RenamePlayerUseCaseTest {

    private val repository = FakeGameRepository()
    private val renamePlayer = RenamePlayerUseCase(repository, fixedClock)

    private val omar = player(seat = 0, score = 70, name = "Omar")
    private val mona = player(seat = 1, score = 30, name = "Mona")

    @Test
    fun renamesAndKeepsIdAndScore() = runTest {
        repository.seed(game(listOf(omar, mona)))

        assertEquals("Omar K.", (renamePlayer(omar.id, " Omar K. ") as Outcome.Success).value.value)

        val renamed = repository.activeGame().player(omar.id)
        assertEquals("Omar K.", renamed?.name?.value)
        assertEquals(70, renamed?.score)
    }

    @Test
    fun rejectsAnInvalidName() = runTest {
        repository.seed(game(listOf(omar, mona)))

        assertEquals(Outcome.Failure(InvalidName(PlayerNameError.BLANK)), renamePlayer(omar.id, " "))
    }

    @Test
    fun rejectsTheNameOfAnotherPlayerIgnoringCase() = runTest {
        repository.seed(game(listOf(omar, mona)))

        assertEquals(Outcome.Failure(NameTaken), renamePlayer(omar.id, "MONA"))
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun changingOnlyTheCaseOfTheOwnNameIsAllowed() = runTest {
        repository.seed(game(listOf(omar, mona)))

        renamePlayer(omar.id, "OMAR")

        assertEquals("OMAR", repository.activeGame().player(omar.id)?.name?.value)
    }

    @Test
    fun savingTheSameNameWritesNothing() = runTest {
        repository.seed(game(listOf(omar, mona)))

        renamePlayer(omar.id, "Omar")

        assertEquals(0, repository.writeCount)
    }

    @Test
    fun rejectsAPlayerFromAnotherGame() = runTest {
        repository.seed(game(listOf(omar, mona)))

        assertEquals(Outcome.Failure(UnknownPlayer), renamePlayer(PlayerId(42), "Sara"))
    }
}
