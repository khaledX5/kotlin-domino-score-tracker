package io.github.khaledx5.dominoscore.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheGameInProgressWithItsStandings() {
        val game = ActiveGameUi(
            roundNumber = 2,
            target = 150,
            standings = listOf(
                StandingUi(playerId = 1, rank = 1, name = "Omar", score = 160, colorIndex = 0, qualified = true),
                StandingUi(playerId = 2, rank = 2, name = "Mona", score = -20, colorIndex = 1, qualified = false),
            ),
        )

        composeRule.setContent { DominoTheme { HomeContent(state = HomeUiState.Ready(game)) } }

        composeRule.onNodeWithText("Game in progress").assertIsDisplayed()
        composeRule.onNodeWithText("Round 2 · Target 150").assertIsDisplayed()
        composeRule.onNodeWithText("Omar").assertIsDisplayed()
        composeRule.onNodeWithText("Qualified").assertIsDisplayed()
        composeRule.onNodeWithText("-20").assertIsDisplayed()
    }

    @Test
    fun hidesTheCardWhenNoGameIsInProgress() {
        composeRule.setContent { DominoTheme { HomeContent(state = HomeUiState.Ready(activeGame = null)) } }

        composeRule.onNodeWithText("Domino Score Tracker").assertIsDisplayed()
        composeRule.onNodeWithText("Game in progress").assertDoesNotExist()
    }
}
