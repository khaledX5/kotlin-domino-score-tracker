package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReactionLabelTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheEventAndGoesAwayWithIt() {
        var reaction by mutableStateOf<GameSoundEvent?>(GameSoundEvent.EQUAL_SCORES)
        composeRule.setContent { DominoTheme { ReactionLabel(reaction = reaction) } }

        composeRule.onNodeWithText("It's a tie! 🤝").assertIsDisplayed()

        reaction = null
        composeRule.waitForIdle()
        composeRule.onNodeWithText("It's a tie! 🤝").assertDoesNotExist()
    }

    @Test
    fun speaksArabic() {
        RuntimeEnvironment.setQualifiers("ar")
        composeRule.setContent { DominoTheme { ReactionLabel(reaction = GameSoundEvent.FINAL_STRETCH) } }

        composeRule.onNodeWithText("آخر 10 نقط! 😬").assertIsDisplayed()
    }
}
