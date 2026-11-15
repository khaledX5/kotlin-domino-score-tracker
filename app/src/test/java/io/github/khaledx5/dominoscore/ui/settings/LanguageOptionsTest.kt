package io.github.khaledx5.dominoscore.ui.settings

import androidx.compose.ui.test.assertIsSelected
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
class LanguageOptionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsEachLanguageInItsOwnScriptAndReportsTheChoice() {
        var picked: AppLanguage? = null
        composeRule.setContent {
            DominoTheme { LanguageOptions(selected = AppLanguage.SYSTEM, onSelect = { picked = it }) }
        }

        composeRule.onNodeWithText("System", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("English", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("العربية", useUnmergedTree = true).performClick()

        assertEquals(AppLanguage.ARABIC, picked)
    }

    @Test
    fun marksTheCurrentLanguageAsSelected() {
        composeRule.setContent {
            DominoTheme { LanguageOptions(selected = AppLanguage.ENGLISH, onSelect = {}) }
        }

        composeRule.onNodeWithText("English").assertIsSelected()
    }
}
