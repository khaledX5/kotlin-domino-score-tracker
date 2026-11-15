package io.github.khaledx5.dominoscore.ui.theme

import androidx.compose.ui.graphics.Color

internal val Indigo = Color(0xFF585A91)
internal val IndigoContainer = Color(0xFFE4E3FA)
internal val OnIndigoContainer = Color(0xFF1B1C4A)
internal val Surface = Color(0xFFFFFFFF)
internal val Background = Color(0xFFF3F3F8)
internal val SurfaceVariant = Color(0xFFF1F1F5)
internal val OnSurface = Color(0xFF1C1B22)
internal val OnSurfaceVariant = Color(0xFF6B6A75)
internal val Outline = Color(0xFFDADAE3)
internal val Error = Color(0xFFD32F2F)

/** Colors with a game meaning that Material's scheme has no slot for. */
data class GameColors(
    val add: Color,
    val addContainer: Color,
    val penalty: Color,
    val penaltyContainer: Color,
    val qualified: Color,
    val qualifiedContainer: Color,
    val players: List<Color>,
)

internal val LightGameColors = GameColors(
    add = Color(0xFF1E9E6A),
    addContainer = Color(0xFFE6F5EE),
    penalty = Color(0xFFF08C00),
    penaltyContainer = Color(0xFFFFF4E5),
    qualified = Color(0xFFFF9800),
    qualifiedContainer = Color(0xFFFFF3D6),
    players = listOf(
        Color(0xFF6366F1),
        Color(0xFFEC4899),
        Color(0xFF14B8A6),
        Color(0xFFF59E0B),
    ),
)
