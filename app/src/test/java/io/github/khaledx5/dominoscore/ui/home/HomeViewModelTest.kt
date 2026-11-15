package io.github.khaledx5.dominoscore.ui.home

import app.cash.turbine.test
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.testClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeGameRepository()
    private val startGame = StartGameUseCase(repository, testClock)
    private val recordScore = RecordScoreUseCase(repository, testClock)

    private fun viewModel() = HomeViewModel(ObserveActiveGameUseCase(repository))

    @Test
    fun showsNoGameCardWhenNothingIsInProgress() = runTest {
        viewModel().uiState.test {
            assertEquals(HomeUiState.Ready(activeGame = null), expectMostRecentItem())
        }
    }

    @Test
    fun showsTheActiveGameWithLiveStandings() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona", "Ali"))
        val omar = repository.activeGame().players.first().id

        viewModel().uiState.test {
            val before = (expectMostRecentItem() as HomeUiState.Ready).activeGame!!
            assertEquals(1, before.roundNumber)
            assertEquals(100, before.target)
            assertEquals(listOf(1, 1, 1), before.standings.map { it.rank })

            recordScore(omar, ScoreOperationType.ADD, 120)

            val after = (awaitItem() as HomeUiState.Ready).activeGame!!
            assertEquals(listOf("Omar", "Mona", "Ali"), after.standings.map { it.name })
            assertEquals(listOf(1, 2, 2), after.standings.map { it.rank })
            assertEquals(listOf(true, false, false), after.standings.map { it.qualified })
        }
    }
}
