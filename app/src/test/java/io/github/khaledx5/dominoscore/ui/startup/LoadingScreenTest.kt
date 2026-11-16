package io.github.khaledx5.dominoscore.ui.startup

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LoadingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showLoading(ready: Boolean = false) {
        composeRule.setContent { DominoTheme { LoadingScreen(ready = ready) } }
    }

    @Test
    fun carriesTheHomeBrandingAndAnHonestIndeterminateBar() {
        showLoading()

        composeRule.onNodeWithText("Domino Score Tracker").assertIsDisplayed()
        composeRule.onNodeWithText("Keep score of every domino game").assertIsDisplayed()
        composeRule.onNodeWithText("Getting things ready…").assertIsDisplayed()
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun saysReadyAndFillsTheBarOnceStartupIsDone() {
        showLoading(ready = true)

        composeRule.onNodeWithText("Ready!").assertIsDisplayed()
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(1f, 0f..1f))).assertIsDisplayed()
    }

    @Test
    fun speaksArabic() {
        RuntimeEnvironment.setQualifiers("ar")
        showLoading()

        composeRule.onNodeWithText("سكور الدومينو").assertIsDisplayed()
        composeRule.onNodeWithText("بنجهّز كل حاجة…").assertIsDisplayed()
    }

    @Test
    fun fitsASmallPhone() = assertEverythingReachable("w320dp-h480dp")

    @Test
    fun fitsAPhoneOnItsSide() = assertEverythingReachable("w640dp-h360dp-land")

    @Test
    fun fitsATablet() = assertEverythingReachable("w800dp-h1280dp")

    private fun assertEverythingReachable(qualifiers: String) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        showLoading()

        listOf("Domino Score Tracker", "Keep score of every domino game", "Getting things ready…").forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
    }
}
