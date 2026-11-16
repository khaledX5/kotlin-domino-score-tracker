package io.github.khaledx5.dominoscore.ui.startup

import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.ui.startup.StartupViewModel.Companion.FINISH_MS
import io.github.khaledx5.dominoscore.ui.startup.StartupViewModel.Companion.INTRO_MS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StartupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()

    /** A database whose first read takes [millis], like a first open that runs a migration. */
    private fun slowDatabase(millis: Long): GameRepository = object : GameRepository by games {
        override fun observeActiveGame(): Flow<Game?> = games.observeActiveGame().onStart { delay(millis) }
    }

    @Test
    fun aFastDatabaseStillShowsTheWholeEntranceThenReady() = runTest {
        val viewModel = StartupViewModel(ObserveActiveGameUseCase(games))

        advanceTimeBy(INTRO_MS - 1)
        assertEquals(StartupUiState.Loading, viewModel.uiState.value)

        advanceTimeBy(2)
        assertEquals(StartupUiState.Finishing, viewModel.uiState.value)

        advanceTimeBy(FINISH_MS)
        assertEquals(StartupUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun aSlowDatabaseKeepsTheLoadingScreenUntilItAnswers() = runTest {
        val viewModel = StartupViewModel(ObserveActiveGameUseCase(slowDatabase(millis = 3_000)))

        advanceTimeBy(2_999)
        assertEquals(StartupUiState.Loading, viewModel.uiState.value)

        advanceTimeBy(2)
        assertEquals(StartupUiState.Finishing, viewModel.uiState.value)

        advanceTimeBy(FINISH_MS)
        assertEquals(StartupUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun theReadRunsAlongsideTheEntranceNotAfterIt() = runTest {
        val viewModel = StartupViewModel(ObserveActiveGameUseCase(slowDatabase(millis = INTRO_MS - 100)))

        advanceTimeBy(INTRO_MS + 1)

        assertEquals(StartupUiState.Finishing, viewModel.uiState.value)
    }
}
