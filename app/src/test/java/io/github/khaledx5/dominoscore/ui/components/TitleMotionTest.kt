package io.github.khaledx5.dominoscore.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.khaledx5.dominoscore.ui.game.PlayerCard
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TitleMotionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aCardIsUnsettledOnlyWhileItIsFirstComposed() {
        val seen = mutableListOf<Boolean>()
        composeRule.setContent {
            val settled = rememberSettled()
            seen += settled
        }
        composeRule.waitForIdle()

        // First display: no title animation. Every later change: a real one.
        assertEquals(listOf(false, true), seen)
    }

    @Test
    fun aTitleWonOnScreenEndsUpShownOnTheCard() {
        var player by mutableStateOf(
            StandingUi(playerId = 1, rank = 2, name = "Omar", score = 10, colorIndex = 0, qualified = false),
        )
        composeRule.setContent { DominoTheme { PlayerCard(player) } }
        composeRule.onNodeWithText("#2").assertIsDisplayed()

        player = player.copy(rank = 1, score = 40, isKing = true)

        composeRule.onNodeWithText("👑 KING").assertIsDisplayed()
        composeRule.onNodeWithText("#2").assertDoesNotExist()
    }
}
