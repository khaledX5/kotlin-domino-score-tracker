package io.github.khaledx5.dominoscore.ui.settings

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.khaledx5.dominoscore.audio.MusicSettings
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameSoundOptionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val changes = mutableListOf<Boolean>()

    private fun show(settings: MusicSettings) {
        composeRule.setContent {
            DominoTheme { GameSoundOptions(settings = settings, onEventSoundsChange = { changes += it }) }
        }
    }

    @Test
    fun oneSwitchTurnsEveryEventSoundOff() {
        show(MusicSettings())

        composeRule.onNodeWithText("Event sounds").assertIsOn().performClick()

        assertEquals(listOf(false), changes)
    }

    @Test
    fun oneSwitchTurnsThemBackOn() {
        show(MusicSettings(eventSoundsEnabled = false))

        composeRule.onNodeWithText("Event sounds").assertIsOff().performClick()

        assertEquals(listOf(true), changes)
    }
}
