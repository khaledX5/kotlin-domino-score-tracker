package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScoreEntrySheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val saved = mutableListOf<Int>()

    private fun show(type: ScoreOperationType) {
        composeRule.setContent {
            DominoTheme {
                ScoreEntrySheet(
                    entry = ScoreEntryUi(playerId = 1, playerName = "Omar", type = type),
                    onAmountChange = {},
                    onQuickAmount = { saved += it },
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }
    }

    @Test
    fun addingOffersEveryAmountFromOneToTen() {
        show(ScoreOperationType.ADD)

        (1..10).forEach { composeRule.onNodeWithText("+$it").assertIsDisplayed() }
        composeRule.onNodeWithText("+15").assertDoesNotExist()

        composeRule.onNodeWithText("+7").performClick()
        assertEquals(listOf(7), saved)
    }

    @Test
    fun aPenaltyOffersTheSameAmounts() {
        show(ScoreOperationType.PENALTY)

        (1..10).forEach { composeRule.onNodeWithText("-$it").assertIsDisplayed() }

        composeRule.onNodeWithText("-10").performClick()
        assertEquals(listOf(10), saved)
    }
}
