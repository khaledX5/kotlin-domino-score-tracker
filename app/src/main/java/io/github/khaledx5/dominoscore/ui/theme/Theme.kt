package io.github.khaledx5.dominoscore.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Midnight Teal on Material 3. Teal is the primary action color; text on it is navy, because
 * white on this teal is below the 4.5:1 contrast minimum.
 */
private val MidnightTealColorScheme = lightColorScheme(
    primary = MidnightTeal.ElectricTeal,
    onPrimary = MidnightTeal.MidnightNavy,
    primaryContainer = MidnightTeal.SoftMint,
    onPrimaryContainer = MidnightTeal.MidnightNavy,
    secondary = MidnightTeal.DeepSlate,
    onSecondary = MidnightTeal.White,
    secondaryContainer = MidnightTeal.DeepSlate,
    onSecondaryContainer = MidnightTeal.White,
    tertiary = MidnightTeal.WarmGold,
    onTertiary = MidnightTeal.MidnightNavy,
    background = MidnightTeal.OffWhite,
    onBackground = MidnightTeal.MidnightNavy,
    surface = MidnightTeal.White,
    onSurface = MidnightTeal.MidnightNavy,
    surfaceVariant = MidnightTeal.OffWhite,
    onSurfaceVariant = MidnightTeal.MutedGray,
    surfaceContainerLow = MidnightTeal.White,
    surfaceContainer = MidnightTeal.White,
    surfaceContainerHigh = MidnightTeal.White,
    surfaceContainerHighest = MidnightTeal.OffWhite,
    inverseSurface = MidnightTeal.MidnightNavy,
    inverseOnSurface = MidnightTeal.OffWhite,
    inversePrimary = MidnightTeal.SoftMint,
    outline = OutlineGray,
    outlineVariant = OutlineGray,
    error = MidnightTeal.Coral,
    onError = MidnightTeal.MidnightNavy,
)

private val LocalGameColors = staticCompositionLocalOf { MidnightTealGameColors }

/** The app ships one light theme, matching the score sheet look players expect at the table. */
@Composable
fun DominoTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGameColors provides MidnightTealGameColors) {
        MaterialTheme(
            colorScheme = MidnightTealColorScheme,
            shapes = DominoShapes,
            content = content,
        )
    }
}

object DominoThemeExtras {
    val gameColors: GameColors
        @Composable get() = LocalGameColors.current
}
