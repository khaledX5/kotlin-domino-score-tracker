package io.github.khaledx5.dominoscore.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnimatedScoreTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theFirstScoreShowsWithoutAFloatingLabel() {
        composeRule.setContent { DominoTheme { AnimatedScore(score = 40) } }

        composeRule.onNodeWithText("40").assertExists()
        composeRule.onNodeWithText("+40").assertDoesNotExist()
    }

    @Test
    fun anAddShowsPlusTheDifferenceAndEndsOnTheNewScore() {
        var score by mutableIntStateOf(40)
        composeRule.setContent { DominoTheme { AnimatedScore(score = score) } }

        score = 55
        composeRule.waitForIdle()

        composeRule.onNodeWithText("+15").assertExists()
        composeRule.onNodeWithText("55").assertExists()
    }

    @Test
    fun aPenaltyShowsMinusTheDifferenceBelowZero() {
        var score by mutableIntStateOf(10)
        composeRule.setContent { DominoTheme { AnimatedScore(score = score) } }

        score = -20
        composeRule.waitForIdle()

        composeRule.onNodeWithText("-30").assertExists()
        composeRule.onNodeWithText("-20").assertExists()
    }
}
