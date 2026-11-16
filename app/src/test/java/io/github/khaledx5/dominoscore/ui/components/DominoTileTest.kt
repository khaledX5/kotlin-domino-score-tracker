package io.github.khaledx5.dominoscore.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DominoTileTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyHalfHasAsManyPipsAsItsValueAllInsideTheHalf() {
        (0..6).forEach { value ->
            val pips = pipCenters(value)
            assertEquals(value, pips.size)
            assertTrue(pips.all { (x, y) -> x in 0.1f..0.9f && y in 0.1f..0.9f })
            assertEquals("pips of $value overlap", pips.size, pips.toSet().size)
        }
    }

    @Test
    fun keepsTheShapeOfARealTile() {
        composeRule.setContent {
            DominoTheme { DominoTile(top = 5, bottom = 6, modifier = Modifier.width(36.dp).testTag(TAG)) }
        }

        composeRule.onNodeWithTag(TAG).assertWidthIsEqualTo(36.dp).assertHeightIsEqualTo(60.dp)
    }

    @Test
    fun refusesAHalfWithMoreThanSixPips() {
        assertThrows(IllegalArgumentException::class.java) {
            composeRule.setContent { DominoTheme { DominoTile(top = 7, bottom = 0) } }
        }
    }

    private companion object {
        const val TAG = "tile"
    }
}
