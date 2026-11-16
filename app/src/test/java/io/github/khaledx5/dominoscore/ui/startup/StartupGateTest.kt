package io.github.khaledx5.dominoscore.ui.startup

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StartupGateTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var state by mutableStateOf(StartupUiState.Loading)

    private fun showGate() {
        composeRule.setContent {
            DominoTheme {
                StartupGate(state = state) {
                    var screen by remember { mutableStateOf("Home") }
                    Button(onClick = { screen = if (screen == "Home") "Game" else "Home" }) { Text(screen) }
                }
            }
        }
    }

    @Test
    fun theLaunchStartsOnTheLoadingScreen() {
        showGate()

        composeRule.onNodeWithText(GETTING_READY).assertIsDisplayed()
        composeRule.onNodeWithText("Home").assertDoesNotExist()
    }

    @Test
    fun itSaysReadyThenFadesIntoTheFirstScreen() {
        showGate()

        state = StartupUiState.Finishing
        composeRule.onNodeWithText(READY).assertIsDisplayed()
        composeRule.onNodeWithText("Home").assertDoesNotExist()

        state = StartupUiState.Ready
        composeRule.waitForIdle()
        composeRule.onNodeWithText(READY).assertDoesNotExist()
        composeRule.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun movingBetweenScreensNeverBringsTheLoadingScreenBack() {
        state = StartupUiState.Ready
        showGate()

        composeRule.onNodeWithText("Home").performClick()
        composeRule.onNodeWithText("Game").performClick()

        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText(GETTING_READY).assertDoesNotExist()
    }

    private companion object {
        const val GETTING_READY = "Getting things ready…"
        const val READY = "Ready!"
    }
}
