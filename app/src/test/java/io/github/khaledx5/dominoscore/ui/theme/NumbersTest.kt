package io.github.khaledx5.dominoscore.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class NumbersTest {

    @Test
    fun numbersAlwaysReadLeftToRightAndKeepTheRestOfTheStyle() {
        val style = TextStyle(fontSize = 24.sp).forNumbers()

        assertEquals(TextDirection.Ltr, style.textDirection)
        assertEquals(24.sp, style.fontSize)
    }
}
