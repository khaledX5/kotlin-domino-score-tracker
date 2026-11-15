package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WinnerDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val candidates = listOf(WinnerCandidateUi(1, "Omar", 160), WinnerCandidateUi(2, "Mona", 160))

    @Test
    fun aTieCannotBeConfirmedBeforeAPick() {
        var picked: Long? = null
        composeRule.setContent {
            DominoTheme {
                WinnerDialog(
                    pick = WinnerPickUi(candidates, tieAtTop = true, selectedId = null),
                    onSelect = { picked = it },
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("It is a tie at the top. Pick the winner.").assertExists()
        composeRule.onNodeWithText("Confirm winner").assertIsNotEnabled()
        composeRule.onNodeWithText("Mona").performClick()
        assertEquals(2L, picked)
    }

    @Test
    fun aSuggestedWinnerCanBeConfirmedStraightAway() {
        var confirmed = 0
        composeRule.setContent {
            DominoTheme {
                WinnerDialog(
                    pick = WinnerPickUi(candidates, tieAtTop = false, selectedId = 1),
                    onSelect = {},
                    onConfirm = { confirmed++ },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Confirm winner").assertIsEnabled().performClick()
        assertEquals(1, confirmed)
    }
}
