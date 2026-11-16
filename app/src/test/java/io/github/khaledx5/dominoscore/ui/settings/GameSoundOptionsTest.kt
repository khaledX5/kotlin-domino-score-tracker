package io.github.khaledx5.dominoscore.ui.settings

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.khaledx5.dominoscore.audio.MusicSettings
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent
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

    private val changes = mutableListOf<Pair<GameSoundEvent, Boolean>>()

    private fun show(settings: MusicSettings) {
        composeRule.setContent {
            DominoTheme {
                GameSoundOptions(
                    settings = settings,
                    onEventSoundsChange = {},
                    onEventSoundChange = { event, on -> changes += event to on },
                    onVisualReactionsChange = {},
                )
            }
        }
    }

    @Test
    fun eachEventHasItsOwnSwitch() {
        show(MusicSettings(mutedEvents = setOf(GameSoundEvent.LARGE_PENALTY)))

        composeRule.onNodeWithText("Players tie above 25").assertIsOn()
        composeRule.onNodeWithText("A penalty above 4").assertIsOff().performClick()

        assertEquals(listOf(GameSoundEvent.LARGE_PENALTY to true), changes)
    }

    @Test
    fun theMasterSwitchGreysOutEveryEvent() {
        show(MusicSettings(eventSoundsEnabled = false))

        composeRule.onNodeWithText("Event sounds").assertIsOff()
        composeRule.onNodeWithText("The last player finally scores").assertIsNotEnabled()
        composeRule.onNodeWithText("Visual reactions").assertIsOn()
    }
}
