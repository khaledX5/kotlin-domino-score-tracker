package io.github.khaledx5.dominoscore.domain.usecase

import app.cash.turbine.test
import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.game
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType.ADD
import io.github.khaledx5.dominoscore.domain.rules.PrimaryAction
import io.github.khaledx5.dominoscore.domain.rules.WinnerSelection
import io.github.khaledx5.dominoscore.domain.scores
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveActiveGameUseCaseTest {

    private val repository = FakeGameRepository()
    private val observeActiveGame = ObserveActiveGameUseCase(repository)
    private val recordScore = RecordScoreUseCase(repository, fixedClock)
    private val confirmWinner = ConfirmWinnerUseCase(repository, fixedClock)

    @Test
    fun emitsNullWhenNoGameIsActive() = runTest {
        observeActiveGame().test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun emitsAFreshSnapshotAfterEveryChangeUntilTheGameEnds() = runTest {
        repository.seed(game(scores(140, 100), target = 150))

        observeActiveGame().test {
            val before = awaitItem()!!
            assertEquals(PrimaryAction.END_ROUND, before.primaryAction)
            assertEquals(listOf(140, 100), before.ranking.map { it.player.score })

            recordScore(PlayerId(1), ADD, 60)
            val after = awaitItem()!!
            assertEquals(listOf(160, 140), after.ranking.map { it.player.score })
            assertEquals(PrimaryAction.END_GAME, after.primaryAction)
            assertEquals(PlayerId(1), (after.winnerSelection as WinnerSelection.Suggested).suggested.id)

            confirmWinner(PlayerId(1))
            assertNull(awaitItem())
        }
    }
}
