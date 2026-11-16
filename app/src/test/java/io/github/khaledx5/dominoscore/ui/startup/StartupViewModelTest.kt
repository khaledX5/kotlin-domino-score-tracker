package io.github.khaledx5.dominoscore.ui.startup

import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.ui.startup.StartupViewModel.Companion.SYSTEM_SPLASH_HOLD_MS
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
    fun aFastDatabaseIsReadyAtOnceAndNeverShowsTheLoadingScreen() = runTest {
        val viewModel = StartupViewModel(ObserveActiveGameUseCase(games))

        assertEquals(StartupUiState.Ready, viewModel.uiState.value)
        advanceTimeBy(SYSTEM_SPLASH_HOLD_MS * 4)
        assertEquals(StartupUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun aReadThatEndsDuringTheHoldGoesStraightFromTheSplashToReady() = runTest {
        val viewModel = StartupViewModel(ObserveActiveGameUseCase(slowDatabase(millis = 300)))

        advanceTimeBy(299)
        assertEquals(StartupUiState.SystemSplash, viewModel.uiState.value)

        advanceTimeBy(2)
        assertEquals(StartupUiState.Ready, viewModel.uiState.value)

        advanceTimeBy(SYSTEM_SPLASH_HOLD_MS * 4)
        assertEquals(StartupUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun aSlowReadHandsOverToTheLoadingScreenAndLeavesItAsSoonAsItIsDone() = runTest {
        val viewModel = StartupViewModel(ObserveActiveGameUseCase(slowDatabase(millis = 2_000)))

        advanceTimeBy(SYSTEM_SPLASH_HOLD_MS - 1)
        assertEquals(StartupUiState.SystemSplash, viewModel.uiState.value)

        advanceTimeBy(2)
        assertEquals(StartupUiState.LoadingScreen, viewModel.uiState.value)

        advanceTimeBy(2_000 - SYSTEM_SPLASH_HOLD_MS - 2)
        assertEquals(StartupUiState.LoadingScreen, viewModel.uiState.value)

        advanceTimeBy(2)
        assertEquals(StartupUiState.Ready, viewModel.uiState.value)
    }
}
