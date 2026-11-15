package io.github.khaledx5.dominoscore.ui.game

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.khaledx5.dominoscore.domain.rules.PrimaryAction
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
                GameContent(game, MusicButtonUi(visible = true), onBack = {
                }, onPlayMusic = {}, onAdd = {}, onPenalty = {}, onEditTarget = {}, onPrimaryAction = {})
            }
        }

        composeRule.onNodeWithText("Round 3").assertIsDisplayed()
        composeRule.onNodeWithText("150 pts").assertIsDisplayed()
        // The score shows on the card and again in the live ranking.
        composeRule.onAllNodesWithText("-5")[0].assertIsDisplayed()
    }

    @Test
    fun theMusicButtonPlaysOnEveryTap() {
        var taps = 0
        composeRule.setContent {
            DominoTheme {
                GameContent(
                    game = game,
                    music = MusicButtonUi(visible = true),
                    onBack = {},
                    onPlayMusic = { taps++ },
                    onAdd = {},
                    onPenalty = {},
                    onEditTarget = {},
                    onPrimaryAction = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Laugh").performClick()
        composeRule.onNodeWithContentDescription("Laugh").performClick()

        // Same button, same label, after every tap: nothing stays switched on.
        assertEquals(2, taps)
    }

    @Test
    fun noMusicButtonWhenGameplayMusicIsOff() {
        composeRule.setContent {
            DominoTheme {
                GameContent(game, MusicButtonUi(visible = false), onBack = {
                }, onPlayMusic = {}, onAdd = {}, onPenalty = {}, onEditTarget = {}, onPrimaryAction = {})
            }
        }

        composeRule.onNodeWithContentDescription("Laugh").assertDoesNotExist()
    }

    @Test
    fun theMainButtonEndsTheRoundUntilSomeoneQualifies() {
        var taps = 0
        var current by mutableStateOf(game)
        composeRule.setContent {
            DominoTheme {
                GameContent(
                    game = current,
                    music = MusicButtonUi(visible = false),
                    onBack = {},
                    onPlayMusic = {},
                    onAdd = {},
                    onPenalty = {},
                    onEditTarget = {},
                    onPrimaryAction = { taps++ },
                )
            }
        }

        composeRule.onNodeWithText("End round").performClick()
        assertEquals(1, taps)

        val omar = players.first().copy(score = 160, qualified = true)
        current = game.copy(
            players = listOf(omar, players.last()),
            standings = listOf(omar, players.last()),
            primaryAction = PrimaryAction.END_GAME,
        )

        composeRule.onNodeWithText("End game").assertIsDisplayed()
        composeRule.onNodeWithText("End round").assertDoesNotExist()
        composeRule.onNodeWithText("Omar reached the target").assertIsDisplayed()
    }

    @Test
    fun theScreenStaysOnDuringAGame() {
        lateinit var view: View
        composeRule.setContent {
            view = LocalView.current
            DominoTheme {
                GameContent(
                    game = game,
                    music = MusicButtonUi(visible = false),
                    onBack = {},
                    onPlayMusic = {},
                    onAdd = {},
                    onPenalty = {},
                    onEditTarget = {},
                    onPrimaryAction = {},
                )
            }
        }

        composeRule.runOnIdle { assertTrue(view.keepScreenOn) }
    }

    @Test
    fun tappingAPlayersNameOpensRename() {
        val renamed = mutableListOf<String>()
        composeRule.setContent {
            DominoTheme {
                GameContent(
                    game = game,
                    music = MusicButtonUi(visible = false),
                    onBack = {},
                    onPlayMusic = {},
                    onAdd = {},
                    onPenalty = {},
                    onEditTarget = {},
                    onPrimaryAction = {},
                    onRename = { renamed += it.name },
                )
            }
        }

        composeRule.onAllNodesWithText("Mona")[0].performClick()

        assertEquals(listOf("Mona"), renamed)
    }
}
