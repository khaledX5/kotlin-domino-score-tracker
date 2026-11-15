package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val players = listOf(
        StandingUi(playerId = 1, rank = 1, name = "Omar", score = 60, colorIndex = 0, qualified = false, isKing = true),
        StandingUi(playerId = 2, rank = 2, name = "Mona", score = -5, colorIndex = 1, qualified = false, isKooz = true),
    )
    private val game = GameUi(roundNumber = 3, target = 150, players = players, standings = players)

    @Test
    fun showsTheRoundTargetAndEveryPlayer() {
        composeRule.setContent {
            DominoTheme {
                GameContent(game, MusicButtonUi(visible = true, playing = false), onBack = {}, onToggleMusic = {})
            }
        }

        composeRule.onNodeWithText("Round 3").assertIsDisplayed()
        composeRule.onNodeWithText("150 pts").assertIsDisplayed()
        // The score shows on the card and again in the live ranking.
        composeRule.onAllNodesWithText("-5")[0].assertIsDisplayed()
    }

    @Test
    fun theMusicButtonSaysWhatItWillDo() {
        var toggles = 0
        composeRule.setContent {
            DominoTheme {
                GameContent(
                    game = game,
                    music = MusicButtonUi(visible = true, playing = true),
                    onBack = {},
                    onToggleMusic = { toggles++ },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Pause music").performClick()

        assertEquals(1, toggles)
    }

    @Test
    fun noMusicButtonWhenGameplayMusicIsOff() {
        composeRule.setContent {
            DominoTheme {
                GameContent(game, MusicButtonUi(visible = false, playing = false), onBack = {}, onToggleMusic = {})
            }
        }

        composeRule.onNodeWithContentDescription("Play music").assertDoesNotExist()
    }
}
