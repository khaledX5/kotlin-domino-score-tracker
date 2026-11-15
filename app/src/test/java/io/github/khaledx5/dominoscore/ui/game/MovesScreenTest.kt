package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.cash.turbine.test
import io.github.khaledx5.dominoscore.domain.history.ObserveMovesUseCase
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.EndRoundUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
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
class MovesScreenTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val games = FakeGameRepository()
    private val recordScore = RecordScoreUseCase(games, testClock)

    private fun viewModel() = MovesViewModel(ObserveActiveGameUseCase(games), ObserveMovesUseCase(games))

    @Test
    fun everyRoundIsListedNewestFirstWithItsNewestMoveOnTop() = runTest {
        StartGameUseCase(games, testClock)(targetScore = 500, playerNames = listOf("Omar", "Mona"))
        val (omar, mona) = games.activeGame().players
        recordScore(omar.id, ScoreOperationType.ADD, 10)
        recordScore(mona.id, ScoreOperationType.ADD, 15)
        EndRoundUseCase(games, testClock)()
        recordScore(omar.id, ScoreOperationType.PENALTY, 4)
        EndRoundUseCase(games, testClock)()
        recordScore(mona.id, ScoreOperationType.ADD, 30)

        viewModel().uiState.test {
            val rounds = (expectMostRecentItem() as MovesUiState.Ready).rounds
            assertEquals(listOf(3, 2, 1), rounds.map { it.number })
            assertEquals(
                listOf(listOf(30), listOf(-4), listOf(15, 10)),
                rounds.map { round ->
                    round.moves.map { it.points }
                },
            )
        }
    }

    @Test
    fun theScreenClosesWhenThereIsNoGame() = runTest {
        viewModel().uiState.test {
            assertEquals(MovesUiState.NoActiveGame, expectMostRecentItem())
        }
    }

    @Test
    fun showsRoundHeadersAndMoves() {
        val state = MovesUiState.Ready(
            listOf(
                MoveRoundUi(2, listOf(MoveUi(3, "Mona", 1, 2, -5, 10))),
                MoveRoundUi(1, listOf(MoveUi(2, "Mona", 1, 1, 15, 15), MoveUi(1, "Omar", 0, 1, 10, 10))),
            ),
        )
        composeRule.setContent { DominoTheme { MovesContent(state = state, onBack = {}) } }

        composeRule.onNodeWithText("Round 2").assertIsDisplayed()
        composeRule.onNodeWithText("Round 1").assertIsDisplayed()
        composeRule.onNodeWithText("-5").assertIsDisplayed()
        assertEquals(2, composeRule.onAllNodesWithText("Mona").fetchSemanticsNodes().size)
    }

    @Test
    fun seeMoreOpensTheFullHistory() {
        var opened = 0
        val players = listOf(
            io.github.khaledx5.dominoscore.ui.components.StandingUi(1, 1, "Omar", 10, 0, false, isKing = true),
        )
        composeRule.setContent {
            DominoTheme {
                GameContent(
                    game = GameUi(
                        roundNumber = 1,
                        target = 100,
                        players = players,
                        standings = players,
                        recentMoves = listOf(MoveUi(1, "Omar", 0, 1, 10, 10)),
                    ),
                    music = MusicButtonUi(visible = false),
                    onBack = {},
                    onPlayMusic = {},
                    onAdd = {},
                    onPenalty = {},
                    onEditTarget = {},
                    onPrimaryAction = {},
                    onSeeAllMoves = { opened++ },
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("See more"))
        composeRule.onNodeWithText("See more").performClick()

        assertEquals(1, opened)
    }
}
