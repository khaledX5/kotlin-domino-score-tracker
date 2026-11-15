package io.github.khaledx5.dominoscore.ui.summary

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import io.github.khaledx5.dominoscore.domain.messages.MessageLibrary
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.testing.FakePlayerMessageRepository
import io.github.khaledx5.dominoscore.domain.usecase.ConfirmWinnerUseCase
import io.github.khaledx5.dominoscore.domain.usecase.GetGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObservePlayerMessagesUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.testClock
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SummaryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val games = FakeGameRepository()

    private val messages = FakePlayerMessageRepository()

    private fun viewModel(gameId: Long) = SummaryViewModel(
        SavedStateHandle(mapOf(SummaryViewModel.GAME_ID_ARG to gameId)),
        GetGameUseCase(games),
        ObservePlayerMessagesUseCase(messages) { MessageLibrary.EMPTY },
        StartGameUseCase(games, testClock),
    )

    private suspend fun finishedGame(): Long {
        StartGameUseCase(games, testClock)(targetScore = 50, playerNames = listOf("Omar", "Mona"))
        val (omar, mona) = games.activeGame().players
        RecordScoreUseCase(games, testClock)(omar.id, ScoreOperationType.ADD, 70)
        RecordScoreUseCase(games, testClock)(mona.id, ScoreOperationType.ADD, 20)
        val gameId = games.activeGame().id.value
        ConfirmWinnerUseCase(games, testClock)(omar.id)
        return gameId
    }

    @Test
    fun showsTheConfirmedWinner() = runTest {
        val ready = viewModel(finishedGame()).uiState.value as SummaryUiState.Ready

        assertEquals("Omar", ready.winner.name)
        assertEquals(70, ready.winner.score)
        assertEquals(listOf("Omar", "Mona"), ready.standings.map { it.name })
    }

    @Test
    fun aMissingGameClosesTheScreen() = runTest {
        assertEquals(SummaryUiState.Unavailable, viewModel(gameId = 42).uiState.value)
    }

    @Test
    fun theWinnerScreenNamesTheWinner() = runTest {
        val ready = viewModel(finishedGame()).uiState.value as SummaryUiState.Ready
        composeRule.setContent {
            DominoTheme { SummaryContent(summary = ready, onClose = {}, onPlayAgain = {}, onNewGame = {}) }
        }

        composeRule.onNodeWithText("Omar wins!").assertIsDisplayed()
        composeRule.onNodeWithText("70 pts").assertIsDisplayed()
        composeRule.onNodeWithText("Final ranking").assertIsDisplayed()
        composeRule.onNodeWithText("👑 KING", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("🪣 KOOZ", useUnmergedTree = true).assertExists()
    }

    @Test
    fun playAgainStartsTheSameTableFromZero() = runTest {
        val finishedId = finishedGame()
        val viewModel = viewModel(finishedId)

        viewModel.playAgain()

        assertEquals(true, (viewModel.uiState.value as SummaryUiState.Ready).replayStarted)
        val replay = games.activeGame()
        assertNotEquals(finishedId, replay.id.value)
        assertEquals(listOf("Omar", "Mona"), replay.players.map { it.name.value })
        assertEquals(listOf(0, 0), replay.players.map { it.score })
        assertEquals(50, replay.target.value)
    }
}
