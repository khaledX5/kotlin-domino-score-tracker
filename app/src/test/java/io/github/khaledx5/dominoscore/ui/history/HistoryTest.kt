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
import io.github.khaledx5.dominoscore.domain.history.ObservePlayerCountHistoryUseCase
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

    private fun historyViewModel() = HistoryViewModel(SavedStateHandle(), ObservePlayerCountHistoryUseCase(games))

    @Test
    fun startsOnTwoPlayersWithOnlyTheirCompletedGames() = runTest {
        playAndFinish()
        StartGameUseCase(games, testClock)(100, listOf("Ali", "Sara"), replaceActiveGame = true)
        StartGameUseCase(games, testClock)(100, listOf("Hana", "Tamer", "Omar"), replaceActiveGame = true)

        historyViewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals(2, state.playerCount)
            val ready = state.content as HistoryContentState.Ready
            // The abandoned 2-player game is not in history; the active 3-player game neither.
            assertEquals(listOf("Omar"), ready.games.map { it.winnerName })
            val game = ready.games.single()
            assertEquals(2, game.rounds)
            assertEquals(55, game.winnerScore)
            assertEquals(listOf("Omar" to true, "Mona" to false), game.standings.map { it.name to it.isKing })
            assertEquals(listOf(false, true), game.standings.map { it.isKooz })
        }
    }

    @Test
    fun switchingThePlayerCountShowsThatCategoryOnly() = runTest {
        playAndFinish()
        val viewModel = historyViewModel()

        viewModel.uiState.test {
            assertEquals(1, (expectMostRecentItem().content as HistoryContentState.Ready).games.size)

            viewModel.selectPlayerCount(3)

            val three = expectMostRecentItem()
            assertEquals(3, three.playerCount)
            assertEquals(emptyList<CompletedGameUi>(), (three.content as HistoryContentState.Ready).games)
        }
    }

    @Test
    fun theListShowsTheWinnerTheRankingAndOpensTheGame() = runTest {
        val gameId = playAndFinish()
        val ready = historyViewModel().uiState.let { flow ->
            var state: HistoryUiState? = null
            flow.test { state = expectMostRecentItem() }
            state!!
        }

        var opened: Long? = null
        var selected: Int? = null
        composeRule.setContent {
            DominoTheme {
                HistoryContent(
                    state = ready,
                    onBack = {},
                    onSelectPlayerCount = { selected = it },
                    onOpenGame = { opened = it },
                )
            }
        }

        composeRule.onNodeWithText("2 rounds", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("👑 KING", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("🪣 KOOZ", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("🏆 Omar won").assertIsDisplayed().performClick()
        assertEquals(gameId, opened)
        composeRule.onNodeWithText("4 players").performClick()
        assertEquals(4, selected)
    }

    @Test
    fun anEmptyCategorySaysWhichTableSizeHasNoGames() {
        composeRule.setContent {
            DominoTheme {
                HistoryContent(
                    state = HistoryUiState(playerCount = 3, content = HistoryContentState.Ready(emptyList())),
                    onBack = {},
                    onSelectPlayerCount = {},
                    onOpenGame = {},
                )
            }
        }

        composeRule.onNodeWithText("No finished games with 3 players yet.").assertIsDisplayed()
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
