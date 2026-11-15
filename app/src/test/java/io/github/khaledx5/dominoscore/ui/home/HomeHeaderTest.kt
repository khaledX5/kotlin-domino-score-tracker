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
class HomeHeaderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsAppNameAndTagline() {
        composeRule.setContent { DominoTheme { HomeHeader() } }

        composeRule.onNodeWithText("Domino Score Tracker").assertIsDisplayed()
        composeRule.onNodeWithText("Keep score of every domino game").assertIsDisplayed()
    }
}
