package io.github.khaledx5.dominoscore.ui.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.khaledx5.dominoscore.domain.history.GetGameDetailUseCase
import io.github.khaledx5.dominoscore.domain.history.ObserveGameHistoryUseCase
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.ConfirmWinnerUseCase
import io.github.khaledx5.dominoscore.domain.usecase.EndRoundUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.testClock
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val games = FakeGameRepository()

    private suspend fun playAndFinish(): Long {
        StartGameUseCase(games, testClock)(targetScore = 50, playerNames = listOf("Omar", "Mona"))
        val (omar, mona) = games.activeGame().players
        val gameId = games.activeGame().id.value
        RecordScoreUseCase(games, testClock)(omar.id, ScoreOperationType.ADD, 30)
        RecordScoreUseCase(games, testClock)(mona.id, ScoreOperationType.ADD, 10)
        EndRoundUseCase(games, testClock)()
        RecordScoreUseCase(games, testClock)(omar.id, ScoreOperationType.ADD, 25)
        ConfirmWinnerUseCase(games, testClock)(omar.id)
        return gameId
    }

    @Test
    fun theListShowsWinnersAndUnfinishedGames() = runTest {
        playAndFinish()
        StartGameUseCase(games, testClock)(targetScore = 100, playerNames = listOf("Ali", "Sara"))
        StartGameUseCase(games, testClock)(100, listOf("Hana", "Tamer"), replaceActiveGame = true)

        HistoryViewModel(ObserveGameHistoryUseCase(games)).uiState.test {
            val ready = expectMostRecentItem() as HistoryUiState.Ready
            assertEquals(listOf(null, "Omar"), ready.games.map { it.winnerName })

            var opened: Long? = null
            composeRule.setContent {
                DominoTheme { HistoryContent(state = ready, onBack = {}, onOpenGame = { opened = it }) }
            }
            composeRule.onNodeWithText("Not finished").assertIsDisplayed()
            composeRule.onNodeWithText("🏆 Omar won").assertIsDisplayed().performClick()
            assertEquals(ready.games.last().gameId, opened)
        }
    }

    @Test
    fun anEmptyHistorySaysSo() {
        composeRule.setContent {
            DominoTheme { HistoryContent(state = HistoryUiState.Ready(emptyList()), onBack = {}, onOpenGame = {}) }
        }

        composeRule.onNodeWithText("Finished games will show up here.").assertIsDisplayed()
    }

    @Test
    fun theDetailShowsEveryRoundInSeatOrder() = runTest {
        val gameId = playAndFinish()

        val viewModel = GameDetailViewModel(
            SavedStateHandle(mapOf(GameDetailViewModel.GAME_ID_ARG to gameId)),
            GetGameDetailUseCase(games),
        )

        val ready = viewModel.uiState.value as GameDetailUiState.Ready
        assertEquals(listOf(1, 2), ready.rounds.map { it.number })
        assertEquals(listOf(30, 10), ready.rounds[0].cells.map { it.points })
        assertEquals(listOf(55, 10), ready.rounds[1].cells.map { it.total })

        composeRule.setContent { DominoTheme { GameDetailContent(state = ready, onBack = {}) } }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Round 2"))
        composeRule.onNodeWithText("Round 2").assertIsDisplayed()
        composeRule.onNodeWithText("+25").assertIsDisplayed()
    }
}
