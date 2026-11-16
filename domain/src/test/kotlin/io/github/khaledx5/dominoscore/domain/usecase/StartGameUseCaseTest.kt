package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.T0
import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerColors
import io.github.khaledx5.dominoscore.domain.model.RangeError
import io.github.khaledx5.dominoscore.domain.model.RoundStatus
import io.github.khaledx5.dominoscore.domain.rules.RosterIssue
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.valueOrFail
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StartGameUseCaseTest {

    private val repository = FakeGameRepository()
    private val startGame = StartGameUseCase(repository, fixedClock)

    @Test
    fun startsAnActiveGameWithZeroScoresAndRoundOne() = runTest {
        val gameId = startGame(targetScore = 150, playerNames = listOf("Omar", " Mona ", "Ali")).valueOrFail()

        val game = repository.game(gameId)
        assertEquals(GameStatus.ACTIVE, game.status)
        assertEquals(150, game.target.value)
        assertEquals(T0, game.startedAt)
        assertEquals(listOf("Omar", "Mona", "Ali"), game.players.map { it.name.value })
        assertEquals(listOf(0, 1, 2), game.players.map { it.seat })
        assertEquals(listOf(0, 0, 0), game.players.map { it.score })
        assertEquals(1, game.currentRound?.number)
        assertEquals(RoundStatus.IN_PROGRESS, game.currentRound?.status)
    }

    @Test
    fun rejectsAnInvalidTargetWithoutWriting() = runTest {
        val result = startGame(targetScore = 0, playerNames = listOf("Omar", "Mona"))

        assertEquals(Outcome.Failure(InvalidTarget(RangeError.TOO_LOW)), result)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun rejectsAnInvalidRosterWithEveryIssue() = runTest {
        val result = startGame(targetScore = 150, playerNames = listOf("Omar", "omar"))

        assertEquals(Outcome.Failure(InvalidRoster(listOf(RosterIssue.DuplicateName(listOf(0, 1))))), result)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun asksForConfirmationWhileAnotherGameIsActive() = runTest {
        startGame(targetScore = 150, playerNames = listOf("Omar", "Mona")).valueOrFail()

        val result = startGame(targetScore = 200, playerNames = listOf("Ali", "Sara"))

        assertEquals(Outcome.Failure(ActiveGameExists), result)
        assertEquals(150, repository.activeGame().target.value)
    }

    @Test
    fun confirmedReplacementAbandonsTheOldGameAndKeepsIt() = runTest {
        val oldId = startGame(targetScore = 150, playerNames = listOf("Omar", "Mona")).valueOrFail()

        val newId = startGame(targetScore = 200, playerNames = listOf("Ali", "Sara"), replaceActiveGame = true)
            .valueOrFail()

        assertEquals(GameStatus.ABANDONED, repository.game(oldId).status)
        assertEquals(newId, repository.activeGame().id)
    }

    @Test
    fun picksTheColorsTheTableChose() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona", "Ali"), colorIndexes = listOf(5, 0, 7))
            .valueOrFail()

        assertEquals(listOf(5, 0, 7), repository.activeGame().players.map { it.colorIndex })
    }

    @Test
    fun withoutAChoiceTheColorsFollowTheSeats() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona")).valueOrFail()

        assertEquals(listOf(0, 1), repository.activeGame().players.map { it.colorIndex })
    }

    @Test
    fun rejectsARepeatedOrUnknownColor() = runTest {
        assertEquals(
            Outcome.Failure(InvalidColors),
            startGame(100, listOf("Omar", "Mona"), colorIndexes = listOf(3, 3)),
        )
        assertEquals(
            Outcome.Failure(InvalidColors),
            startGame(100, listOf("Omar", "Mona"), colorIndexes = listOf(0, PlayerColors.COUNT)),
        )
        assertEquals(Outcome.Failure(InvalidColors), startGame(100, listOf("Omar", "Mona"), colorIndexes = listOf(1)))
        assertEquals(0, repository.writeCount)
    }
}
