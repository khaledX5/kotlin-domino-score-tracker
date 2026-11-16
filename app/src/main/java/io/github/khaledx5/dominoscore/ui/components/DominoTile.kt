package io.github.khaledx5.dominoscore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras

/**
 * One upright domino drawn on a canvas: two halves of 0 to 6 pips split by a line, like the app
 * icon. It has no fixed size; give it a width and the height follows the 3:5 shape of a real tile.
 * Decorative only, so it has no semantics.
 */
@Composable
fun DominoTile(
    top: Int,
    bottom: Int,
    modifier: Modifier = Modifier,
    color: Color = DominoThemeExtras.gameColors.brand,
    pipColor: Color = DominoThemeExtras.gameColors.onBrand,
) {
    require(top in PIP_RANGE && bottom in PIP_RANGE) { "A domino half has 0 to 6 pips, got $top and $bottom" }
    Canvas(modifier = modifier.aspectRatio(TILE_ASPECT_RATIO)) {
        drawDomino(top = top, bottom = bottom, color = color, pipColor = pipColor)
    }
}

private fun DrawScope.drawDomino(top: Int, bottom: Int, color: Color, pipColor: Color) {
    val width = size.width
    val half = size.height / 2
    drawRoundRect(color = color, cornerRadius = CornerRadius(width * CORNER_FRACTION))
    val inset = width * DIVIDER_INSET_FRACTION
    drawLine(
        color = pipColor,
        start = Offset(inset, half),
        end = Offset(width - inset, half),
        strokeWidth = width * DIVIDER_WIDTH_FRACTION,
        cap = StrokeCap.Round,
    )
    drawPips(count = top, area = Rect(0f, 0f, width, half), color = pipColor)
    drawPips(count = bottom, area = Rect(0f, half, width, size.height), color = pipColor)
}

private fun DrawScope.drawPips(count: Int, area: Rect, color: Color) {
    val radius = area.width * PIP_RADIUS_FRACTION
    pipCenters(count).forEach { (x, y) ->
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(
                area.left + area.width * x,
                area.top + area.height * y,
            ),
        )
    }
}

/** Where the pips of one half sit, as fractions of that half's width and height. */
internal fun pipCenters(count: Int): List<Pair<Float, Float>> = PIP_LAYOUTS[count]

private const val NEAR = 0.28f
private const val FAR = 0.72f
private const val MIDDLE = 0.5f
private const val SIX_TOP = 0.22f
private const val SIX_BOTTOM = 0.78f

private val PIP_LAYOUTS: List<List<Pair<Float, Float>>> = listOf(
    emptyList(),
    listOf(MIDDLE to MIDDLE),
    listOf(FAR to NEAR, NEAR to FAR),
    listOf(FAR to NEAR, MIDDLE to MIDDLE, NEAR to FAR),
    listOf(NEAR to NEAR, FAR to NEAR, NEAR to FAR, FAR to FAR),
    listOf(NEAR to NEAR, FAR to NEAR, MIDDLE to MIDDLE, NEAR to FAR, FAR to FAR),
    listOf(NEAR to SIX_TOP, FAR to SIX_TOP, NEAR to MIDDLE, FAR to MIDDLE, NEAR to SIX_BOTTOM, FAR to SIX_BOTTOM),
)

private val PIP_RANGE = 0..6
private const val TILE_ASPECT_RATIO = 0.6f
private const val CORNER_FRACTION = 0.2f
private const val DIVIDER_INSET_FRACTION = 0.18f
private const val DIVIDER_WIDTH_FRACTION = 0.06f
private const val PIP_RADIUS_FRACTION = 0.085f
