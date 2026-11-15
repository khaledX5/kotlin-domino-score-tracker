package io.github.khaledx5.dominoscore.ui.theme

import androidx.annotation.FontRes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.khaledx5.dominoscore.R

private val UsedWeights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

/** One variable font file serves every weight, so the APK carries a single file per script. */
private fun variableFontFamily(@FontRes resId: Int): FontFamily = FontFamily(
    UsedWeights.map { weight ->
        Font(
            resId = resId,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

/** Inter for English. */
val InterFontFamily: FontFamily = variableFontFamily(R.font.inter)

/** Cairo for Arabic: proper Arabic shaping, and Latin glyphs that sit well next to it. */
val CairoFontFamily: FontFamily = variableFontFamily(R.font.cairo)

/**
 * Weights by role: Bold for scores and big numbers (display, headline), SemiBold for section
 * headings (title), Medium for buttons and labels, Regular for body text.
 */
internal fun dominoTypography(family: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.with(weight: FontWeight) = copy(fontFamily = family, fontWeight = weight)
    return Typography(
        displayLarge = base.displayLarge.with(FontWeight.Bold),
        displayMedium = base.displayMedium.with(FontWeight.Bold),
        displaySmall = base.displaySmall.with(FontWeight.Bold),
        headlineLarge = base.headlineLarge.with(FontWeight.Bold),
        headlineMedium = base.headlineMedium.with(FontWeight.Bold),
        headlineSmall = base.headlineSmall.with(FontWeight.Bold),
        titleLarge = base.titleLarge.with(FontWeight.SemiBold),
        titleMedium = base.titleMedium.with(FontWeight.SemiBold),
        titleSmall = base.titleSmall.with(FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.with(FontWeight.Normal),
        bodyMedium = base.bodyMedium.with(FontWeight.Normal),
        bodySmall = base.bodySmall.with(FontWeight.Normal),
        labelLarge = base.labelLarge.with(FontWeight.Medium),
        labelMedium = base.labelMedium.with(FontWeight.Medium),
        labelSmall = base.labelSmall.with(FontWeight.Medium),
    )
}

internal val EnglishTypography = dominoTypography(InterFontFamily)
internal val ArabicTypography = dominoTypography(CairoFontFamily)
