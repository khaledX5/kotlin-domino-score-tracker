package io.github.khaledx5.dominoscore.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = OnIndigoContainer,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    error = Error,
)

private val LocalGameColors = staticCompositionLocalOf { LightGameColors }

/**
 * The app ships a single light theme, matching the score sheet look players expect at the table.
 */
@Composable
fun DominoTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGameColors provides LightGameColors) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            content = content,
        )
    }
}

object DominoThemeExtras {
    val gameColors: GameColors
        @Composable get() = LocalGameColors.current
}
