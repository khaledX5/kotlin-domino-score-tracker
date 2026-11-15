package io.github.khaledx5.dominoscore.ui.game

import app.cash.turbine.test
import io.github.khaledx5.dominoscore.audio.MusicController
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.FakeMusicSettingsStore
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.RecordingAudio
import io.github.khaledx5.dominoscore.testing.testClock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val settings = FakeMusicSettingsStore()
    private val music = MusicController(
        audio = RecordingAudio().manager,
        settingsStore = settings,
        observeActiveGame = ObserveActiveGameUseCase(games),
        scope = TestScope(mainDispatcherRule.dispatcher),
    )
    private val startGame = StartGameUseCase(games, testClock)
    private val recordScore = RecordScoreUseCase(games, testClock)

    private fun viewModel() = GameViewModel(ObserveActiveGameUseCase(games), music)

    @Test
    fun keepsCardsInSeatOrderAndTheRankingInScoreOrder() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona", "Ali"))
        recordScore(games.activeGame().players[2].id, ScoreOperationType.ADD, 30)

        viewModel().uiState.test {
            val game = (expectMostRecentItem() as GameUiState.Playing).game
            assertEquals(listOf("Omar", "Mona", "Ali"), game.players.map { it.name })
            assertEquals(listOf("Ali", "Omar", "Mona"), game.standings.map { it.name })
            assertTrue(game.players.last().isKing)
        }
    }

    @Test
    fun theMusicButtonShowsThePlayingState() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertFalse((expectMostRecentItem() as GameUiState.Playing).music.playing)

            viewModel.toggleMusic()

            assertTrue((awaitItem() as GameUiState.Playing).music.playing)
        }
    }

    @Test
    fun theMusicButtonIsHiddenWhenGameplayMusicIsOff() = runTest {
        settings.setGameplayEnabled(false)
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))

        viewModel().uiState.test {
            assertFalse((expectMostRecentItem() as GameUiState.Playing).music.visible)
        }
    }

    @Test
    fun reportsWhenThereIsNoGameAnyMore() = runTest {
        viewModel().uiState.test {
            assertEquals(GameUiState.NoActiveGame, expectMostRecentItem())
        }
    }
}
