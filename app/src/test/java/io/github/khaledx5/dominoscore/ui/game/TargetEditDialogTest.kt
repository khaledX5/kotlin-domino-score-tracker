package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class TargetEditDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showDialog() = composeRule.setContent {
        DominoTheme {
            TargetEditDialog(edit = TargetEditUi(currentTarget = 100, text = "100"), onTextChange = {
            }, onConfirm = {}, onDismiss = {})
        }
    }

    @Test
    fun changingTheTargetWarnsItIsGoingToBeALongNight() {
        showDialog()

        composeRule.onNodeWithText("It's gonna be a long night! 😂").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "ar")
    fun inArabicTheNightIsLongToo() {
        showDialog()

        composeRule.onNodeWithText("السهرة صباااحى 😂").assertIsDisplayed()
    }
}
