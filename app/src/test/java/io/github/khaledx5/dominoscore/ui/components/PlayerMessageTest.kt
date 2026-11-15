package io.github.khaledx5.dominoscore.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory
import io.github.khaledx5.dominoscore.domain.messages.MessageTone
import io.github.khaledx5.dominoscore.domain.messages.RankingMessage
import io.github.khaledx5.dominoscore.ui.game.PlayerCard
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerMessageTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val kingLine =
        RankingMessage("first_time_king_001", MessageCategory.FIRST_TIME_KING, "ركز عشان مش دايمة 😂")
    private val koozLine = RankingMessage("first_time_kooz_001", MessageCategory.FIRST_TIME_KOOZ, "على الله حكايته 😂")
    private val neutralLine =
        RankingMessage("escaped_kooz_001", MessageCategory.ESCAPED_KOOZ, "أخيرًا شوفنا وشك يا باشا 😂")

    @Test
    fun aKingLineOnlyShowsWhileThePlayerIsKing() {
        assertEquals(MessageTone.KING, kingLine.toUiFor(isKing = true, isKooz = false)?.tone)
        assertNull(kingLine.toUiFor(isKing = false, isKooz = false))
    }

    @Test
    fun aKoozLineOnlyShowsWhileThePlayerIsKooz() {
        assertEquals(MessageTone.KOOZ, koozLine.toUiFor(isKing = false, isKooz = true)?.tone)
        assertNull(koozLine.toUiFor(isKing = true, isKooz = false))
    }

    @Test
    fun aNeutralLineStaysWhateverTheTitle() {
        assertEquals(MessageTone.NEUTRAL, neutralLine.toUiFor(isKing = false, isKooz = false)?.tone)
    }

    @Test
    fun theCardShowsTheLineAndSwapsItWhenANewOneArrives() {
        val omar = StandingUi(playerId = 1, rank = 2, name = "Omar", score = 20, colorIndex = 0, qualified = false)
        var player by mutableStateOf(omar.copy(isKooz = true, message = koozLine.toUiFor(false, true)))
        composeRule.setContent { DominoTheme { PlayerCard(player) } }

        composeRule.onNodeWithText("على الله حكايته 😂").assertIsDisplayed()

        player = omar.copy(rank = 1, isKing = true, message = kingLine.toUiFor(true, false))

        composeRule.onNodeWithText("ركز عشان مش دايمة 😂").assertIsDisplayed()
        composeRule.onNodeWithText("على الله حكايته 😂").assertDoesNotExist()
    }

    @Test
    fun noLineMeansNothingExtraOnTheCard() {
        composeRule.setContent {
            DominoTheme {
                PlayerCard(StandingUi(1, rank = 1, name = "Omar", score = 0, colorIndex = 0, qualified = false))
            }
        }

        composeRule.onNodeWithText("Omar").assertIsDisplayed()
        composeRule.onNodeWithText("على الله حكايته 😂").assertDoesNotExist()
    }
}
