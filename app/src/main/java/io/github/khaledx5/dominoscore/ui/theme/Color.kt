package io.github.khaledx5.dominoscore.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * The Midnight Teal palette: the only raw colors in the app. Screens never use these directly;
 * they read the Material color scheme or [GameColors], which are built from them below.
 */
internal object MidnightTeal {
    val MidnightNavy = Color(0xFF101B2D)
    val DeepSlate = Color(0xFF1C2B40)
    val ElectricTeal = Color(0xFF14B8A6)
    val SoftMint = Color(0xFF99F6E4)
    val WarmGold = Color(0xFFF4B942)
    val Coral = Color(0xFFF07866)
    val OffWhite = Color(0xFFF5F7FA)
    val MutedGray = Color(0xFF94A3B8)
    val White = Color(0xFFFFFFFF)
}

/**
 * Extra colors that only mark players (dots, avatars), so a table of four has enough to choose
 * from. They never color the interface itself, which stays Midnight Teal. No purple, by brand rule.
 */
internal object PlayerOnlyColors {
    val SkyBlue = Color(0xFF38BDF8)
    val Lime = Color(0xFF84CC16)
    val Orange = Color(0xFFF97316)
    val Pink = Color(0xFFEC4899)
}

/** A light wash of a palette color on white, for badge and highlight backgrounds. */
private fun Color.tint(strength: Float): Color = copy(alpha = strength).compositeOver(MidnightTeal.White)

private const val CONTAINER_TINT = 0.16f
private const val OUTLINE_TINT = 0.35f

internal val OutlineGray = MidnightTeal.MutedGray.tint(OUTLINE_TINT)

/** Colors with a game meaning that Material's scheme has no slot for. */
@Immutable
data class GameColors(
    val brand: Color,
    val onBrand: Color,
    val add: Color,
    val addContainer: Color,
    val penalty: Color,
    val penaltyContainer: Color,
    val qualified: Color,
    val qualifiedContainer: Color,
    val king: Color,
    val kingContainer: Color,
    val kooz: Color,
    val koozContainer: Color,
    val players: List<Color>,
)

internal val MidnightTealGameColors = GameColors(
    brand = MidnightTeal.MidnightNavy,
    onBrand = MidnightTeal.White,
    add = MidnightTeal.ElectricTeal,
    addContainer = MidnightTeal.ElectricTeal.tint(CONTAINER_TINT),
    penalty = MidnightTeal.Coral,
    penaltyContainer = MidnightTeal.Coral.tint(CONTAINER_TINT),
    qualified = MidnightTeal.WarmGold,
    qualifiedContainer = MidnightTeal.WarmGold.tint(CONTAINER_TINT),
    king = MidnightTeal.WarmGold,
    kingContainer = MidnightTeal.WarmGold.tint(CONTAINER_TINT),
    kooz = MidnightTeal.Coral,
    koozContainer = MidnightTeal.Coral.tint(CONTAINER_TINT),
    players = listOf(
        MidnightTeal.ElectricTeal,
        MidnightTeal.Coral,
        MidnightTeal.WarmGold,
        MidnightTeal.DeepSlate,
        PlayerOnlyColors.SkyBlue,
        PlayerOnlyColors.Lime,
        PlayerOnlyColors.Orange,
        PlayerOnlyColors.Pink,
    ),
)
