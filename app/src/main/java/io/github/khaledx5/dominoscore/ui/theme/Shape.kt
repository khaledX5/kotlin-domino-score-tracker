package io.github.khaledx5.dominoscore.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii used across the app: extraSmall for badges, small for icon tiles, medium for
 * choice tiles and buttons, large for cards, extraLarge for sheets and dialogs.
 */
internal val DominoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
