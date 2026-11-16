package io.github.khaledx5.dominoscore.ui.flows

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.ObserveKnownPlayersUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.testClock
import io.github.khaledx5.dominoscore.ui.setup.SetupScreen
import io.github.khaledx5.dominoscore.ui.setup.SetupViewModel
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The setup screen driven like a user would, on a real ViewModel and an in-memory repository. */
@RunWith(RobolectricTestRunner::class)
class SetupFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val games = FakeGameRepository()
    private val startGame = StartGameUseCase(games, testClock)
    private var started = false

    private fun showSetup() {
        val viewModel = SetupViewModel(startGame, ObserveKnownPlayersUseCase(games))
        composeRule.setContent {
            DominoTheme { SetupScreen(onBack = {}, onGameStart = { started = true }, viewModel = viewModel) }
        }
    }

    private fun typeName(placeholder: String, name: String) =
        composeRule.onNodeWithText(placeholder).performScrollTo().performTextInput(name)

    @Test
    fun aFilledFormStartsTheGameWithTheChosenTargetAndSeats() {
        showSetup()

        composeRule.onNodeWithText("71").performScrollTo().performClick()
        composeRule.onNodeWithText("3").performScrollTo().performClick()
        typeName("Player 1", "Omar")
        typeName("Player 2", "Mona")
        typeName("Player 3", "Ali")
        composeRule.onNodeWithText("Start game").performClick()
        composeRule.waitForIdle()

        assertTrue(started)
        val game = games.activeGame()
        assertEquals(71, game.target.value)
        assertEquals(listOf("Omar", "Mona", "Ali"), game.players.map { it.name.value })
    }

    @Test
    fun theSameNameTwiceIsMarkedAndNothingStarts() {
        showSetup()

        typeName("Player 1", "Omar")
        typeName("Player 2", "omar")
        composeRule.onNodeWithText("Start game").performClick()
        composeRule.waitForIdle()

        assertEquals(
            2,
            composeRule.onAllNodesWithText("Another player already has this name").fetchSemanticsNodes().size,
        )
        assertFalse(started)
        assertTrue(games.allGames().isEmpty())
    }

    @Test
    fun aPlayerFromAnEarlierGameIsOneTapAway() = runTest {
        startGame(100, listOf("Sara", "Hana"))
        showSetup()

        composeRule.onNodeWithText("Played before").performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText("Hana")).performScrollTo().performClick()
        typeName("Player 2", "Omar")
        composeRule.onNodeWithText("Start game").performClick()
        composeRule.onNodeWithText("Start a new game?").assertIsDisplayed()
        composeRule.onNodeWithText("Start new game").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("Hana", "Omar"), games.activeGame().players.map { it.name.value })
    }
}
