package io.github.khaledx5.dominoscore.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.history.HistoryContent
import io.github.khaledx5.dominoscore.ui.history.HistoryContentState
import io.github.khaledx5.dominoscore.ui.history.HistoryUiState
import io.github.khaledx5.dominoscore.ui.history.StatisticsUi
import io.github.khaledx5.dominoscore.ui.home.HomeContent
import io.github.khaledx5.dominoscore.ui.home.HomeUiState
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ar")
class ArabicLocaleTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun homeIsInArabicWithBothShortcuts() {
        composeRule.setContent {
            DominoTheme {
                HomeContent(state = HomeUiState.Ready(activeGame = null), onNewGame = {
                }, onContinueGame = {}, onOpenSettings = {})
            }
        }

        composeRule.onNodeWithText("سكور الدومينو").assertIsDisplayed()
        composeRule.onNodeWithText("دور جديد").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("سجل الأدوار").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("الإعدادات").assertIsDisplayed()
    }

    @Test
    fun historyTabsAndEmptyTextAreInArabic() {
        composeRule.setContent {
            DominoTheme {
                HistoryContent(
                    state = HistoryUiState(
                        playerCount = 3,
                        content = HistoryContentState.Ready(emptyList(), StatisticsUi(0, 0, emptyList())),
                    ),
                    onBack = {},
                    onSelectPlayerCount = {},
                    onOpenGame = {},
                )
            }
        }

        composeRule.onNodeWithText("السجل").assertIsDisplayed()
        composeRule.onNodeWithText("الإحصائيات").assertIsDisplayed()
        composeRule.onNodeWithText("لاعبين").assertIsDisplayed()
    }

    @Test
    fun arabicPluralsPickTheRightForm() {
        assertEquals("جولة واحدة", resources.getQuantityString(R.plurals.history_rounds, 1, 1))
        assertEquals("جولتين", resources.getQuantityString(R.plurals.history_rounds, 2, 2))
        // The digits follow the device's number system, so only the word is checked here.
        assertTrue(resources.getQuantityString(R.plurals.history_rounds, 5, 5).endsWith("جولات"))
        assertTrue(resources.getQuantityString(R.plurals.history_rounds, 12, 12).endsWith("جولة"))
    }
}
