package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.T0
import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.game
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.RoundStatus
import io.github.khaledx5.dominoscore.domain.scores
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EndRoundUseCaseTest {

    private val repository = FakeGameRepository()
    private val endRound = EndRoundUseCase(repository, fixedClock)

    @Test
    fun completesTheRoundOpensTheNextAndKeepsScores() = runTest {
        repository.seed(game(scores(90, 40), target = 150))

        assertEquals(Outcome.Success(1), endRound())

        val completed = repository.completedRounds.single()
        assertEquals(RoundStatus.COMPLETED, completed.status)
        assertEquals(T0, completed.completedAt)
        val game = repository.activeGame()
        assertEquals(2, game.currentRound?.number)
        assertEquals(RoundStatus.IN_PROGRESS, game.currentRound?.status)
        assertEquals(listOf(90, 40), game.players.map { it.score })
    }

    @Test
    fun isBlockedOnceSomeoneIsQualified() = runTest {
        repository.seed(game(scores(150, 40), target = 150))

        assertEquals(Outcome.Failure(PlayerQualified), endRound())
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun failsWithoutAnActiveGame() = runTest {
        assertEquals(Outcome.Failure(NoActiveGame), endRound())
    }
}
