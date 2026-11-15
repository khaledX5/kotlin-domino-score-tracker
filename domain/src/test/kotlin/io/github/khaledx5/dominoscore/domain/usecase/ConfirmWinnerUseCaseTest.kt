package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.T0
import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.game
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.scores
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfirmWinnerUseCaseTest {

    private val repository = FakeGameRepository()
    private val confirmWinner = ConfirmWinnerUseCase(repository, fixedClock)

    @Test
    fun completesTheGameWithTheWinnerAndTheLastRound() = runTest {
        repository.seed(game(scores(40, 160), target = 150))

        assertEquals(Outcome.Success(Unit), confirmWinner(PlayerId(1)))

        val game = repository.game(GameId(1))
        assertEquals(GameStatus.COMPLETED, game.status)
        assertEquals(PlayerId(1), game.winnerId)
        assertEquals(T0, game.completedAt)
        assertNull(game.currentRound)
        assertEquals(1, repository.completedRounds.single().number)
    }

    @Test
    fun anotherQualifiedPlayerThanTheSuggestionMayWin() = runTest {
        repository.seed(game(scores(150, 190), target = 150))

        assertEquals(Outcome.Success(Unit), confirmWinner(PlayerId(0)))
    }

    @Test
    fun rejectsAPlayerBelowTheTarget() = runTest {
        repository.seed(game(scores(40, 160), target = 150))

        assertEquals(Outcome.Failure(NotACandidate), confirmWinner(PlayerId(0)))
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun onATieOnlyTheTiedPlayersMayWin() = runTest {
        repository.seed(game(scores(170, 155, 170), target = 150))

        assertEquals(Outcome.Failure(NotACandidate), confirmWinner(PlayerId(1)))
        assertEquals(Outcome.Success(Unit), confirmWinner(PlayerId(2)))
    }

    @Test
    fun rejectsConfirmationBeforeAnyoneQualifies() = runTest {
        repository.seed(game(scores(40, 60), target = 150))

        assertEquals(Outcome.Failure(NobodyQualified), confirmWinner(PlayerId(1)))
    }

    @Test
    fun failsWithoutAnActiveGame() = runTest {
        assertEquals(Outcome.Failure(NoActiveGame), confirmWinner(PlayerId(1)))
    }
}
