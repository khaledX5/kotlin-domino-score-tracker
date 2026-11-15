package io.github.khaledx5.dominoscore.ui.results

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.EndRoundUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.testClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoundResultsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val startGame = StartGameUseCase(games, testClock)
    private val recordScore = RecordScoreUseCase(games, testClock)
    private val endRound = EndRoundUseCase(games, testClock)

    private fun viewModel(roundNumber: Int) = RoundResultsViewModel(
        SavedStateHandle(mapOf(RoundResultsViewModel.ROUND_NUMBER_ARG to roundNumber)),
        ObserveActiveGameUseCase(games),
    )

    @Test
    fun showsTheCumulativeStandingsAfterTheRound() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona", "Ali"))
        val (omar, mona, _) = games.activeGame().players
        recordScore(mona.id, ScoreOperationType.ADD, 30)
        recordScore(omar.id, ScoreOperationType.PENALTY, 10)
        endRound()

        viewModel(roundNumber = 1).uiState.test {
            val results = expectMostRecentItem() as RoundResultsUiState.Ready
            assertEquals(1, results.roundNumber)
            assertEquals(2, results.nextRound)
            assertEquals(listOf("Mona", "Ali", "Omar"), results.standings.map { it.name })
            assertEquals(listOf(30, 0, -10), results.standings.map { it.score })
            assertTrue(results.standings.first().isKing)
            assertTrue(results.standings.last().isKooz)
        }
    }

    @Test
    fun closesWhenThereIsNoActiveGame() = runTest {
        viewModel(roundNumber = 1).uiState.test {
            assertEquals(RoundResultsUiState.NoActiveGame, expectMostRecentItem())
        }
    }
}
