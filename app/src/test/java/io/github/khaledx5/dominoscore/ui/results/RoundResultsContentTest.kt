package io.github.khaledx5.dominoscore.ui.results

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoundResultsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val results = RoundResultsUiState.Ready(
        roundNumber = 2,
        nextRound = 3,
        standings = listOf(
            StandingUi(1, rank = 1, name = "Omar", score = 85, colorIndex = 0, qualified = false, isKing = true),
            StandingUi(2, rank = 2, name = "Mona", score = -15, colorIndex = 1, qualified = false, isKooz = true),
        ),
    )

    @Test
    fun everyCardEndsOnItsScoreWithItsTitle() {
        composeRule.setContent { DominoTheme { RoundResultsContent(results, onContinue = {}) } }

        composeRule.onNodeWithText("End of round 2").assertIsDisplayed()
        composeRule.onNodeWithText("Scores carry on into round 3.").assertIsDisplayed()
        // The counting and the title reveal finish on their own, without any tap.
        composeRule.onNodeWithText("85").assertIsDisplayed()
        composeRule.onNodeWithText("-15").assertIsDisplayed()
        composeRule.onNodeWithText("👑 KING").assertIsDisplayed()
        composeRule.onNodeWithText("🪣 KOOZ").assertIsDisplayed()
    }

    @Test
    fun continueReturnsToTheGame() {
        var continued = 0
        composeRule.setContent { DominoTheme { RoundResultsContent(results, onContinue = { continued++ }) } }

        composeRule.onNodeWithText("Continue game").performClick()

        assertEquals(1, continued)
    }
}
