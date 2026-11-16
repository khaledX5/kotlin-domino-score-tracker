package io.github.khaledx5.dominoscore.ui.startup

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.components.DominoTile
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shown on every launch while the app gets ready. It carries the same icon, title and tagline as the
 * home screen, in the same styles, so fading into home feels like one screen settling. [ready] fills
 * the bar and says so.
 */
@Composable
fun LoadingScreen(ready: Boolean, modifier: Modifier = Modifier) {
    val intro = rememberLoadingIntroState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandHeader(intro)
            Spacer(Modifier.height(Spacing.xxl + Spacing.l))
            DominoRow(intro)
            Spacer(Modifier.height(Spacing.xxl))
            LoadingBar(
                ready = ready,
                modifier = Modifier
                    .widthIn(max = BarMaxWidth)
                    .fillMaxWidth()
                    .height(BarHeight)
                    .graphicsLayer { alpha = intro.progress.value },
            )
            Spacer(Modifier.height(Spacing.l))
            Crossfade(
                targetState = ready,
                modifier = Modifier.graphicsLayer { alpha = intro.progress.value },
                label = "loadingMessage",
            ) { isReady ->
                Text(
                    text = stringResource(if (isReady) R.string.loading_ready else R.string.loading_getting_ready),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** The home screen's header, part by part, so each part can enter on its own. */
@Composable
private fun BrandHeader(intro: LoadingIntroState, modifier: Modifier = Modifier) {
    val gameColors = DominoThemeExtras.gameColors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val shown = intro.icon.value
                    alpha = shown
                    scaleX = lerp(ICON_START_SCALE, 1f, shown)
                    scaleY = scaleX
                }
                .size(IconSize)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(gameColors.brand),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = gameColors.onBrand,
                modifier = Modifier.size(IconSize),
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { riseIn(intro.title.value) },
        )
        Text(
            text = stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { riseIn(intro.tagline.value) },
        )
    }
}

/** Fades in while rising a few pixels into place. */
private fun GraphicsLayerScope.riseIn(shown: Float) {
    alpha = shown
    translationY = (1f - shown) * RiseDistance.toPx()
}

/**
 * Three tiles that slide in from the start side, tilted, and straighten as they settle. The teal
 * accents open on both sides once the last one lands.
 */
@Composable
private fun DominoRow(intro: LoadingIntroState, modifier: Modifier = Modifier) {
    val accentColor = MaterialTheme.colorScheme.primary
    // -1 brings the tiles in from the left; in Arabic they come from the right.
    val fromStart = if (LocalLayoutDirection.current == LayoutDirection.Ltr) -1f else 1f
    Row(
        modifier = modifier
            .drawWithContent {
                drawContent()
                drawAccents(shown = intro.accents.value, color = accentColor)
            }
            .padding(horizontal = AccentRoom),
        horizontalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        LoadingTiles.forEachIndexed { index, (top, bottom) ->
            val tile = intro.tiles[index]
            DominoTile(
                top = top,
                bottom = bottom,
                modifier = Modifier
                    .width(TileWidth)
                    .graphicsLayer {
                        val shown = tile.value
                        val away = 1f - shown
                        alpha = shown.coerceIn(0f, 1f)
                        translationX = fromStart * away * TileTravel.toPx()
                        rotationZ = -fromStart * away * TILE_TILT_DEGREES
                    },
            )
        }
    }
}

/** Three short teal strokes fanning out on each side of the tiles, growing outwards. */
private fun DrawScope.drawAccents(shown: Float, color: Color) {
    if (shown <= 0f) return
    val centerY = size.height / 2
    val room = AccentRoom.toPx()
    val gap = AccentGap.toPx()
    val length = AccentLength.toPx() * shown
    val stroke = AccentStroke.toPx()
    val lineColor = color.copy(alpha = shown.coerceIn(0f, 1f))
    listOf(-1f to room, 1f to size.width - room).forEach { (side, edge) ->
        ACCENT_ANGLES.forEach { degrees ->
            val radians = Math.toRadians(degrees.toDouble())
            val dx = side * cos(radians).toFloat()
            val dy = sin(radians).toFloat()
            drawLine(
                color = lineColor,
                start = Offset(edge + dx * gap, centerY + dy * gap),
                end = Offset(edge + dx * (gap + length), centerY + dy * (gap + length)),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** The tiles shown, as top and bottom pips. */
private val LoadingTiles = listOf(1 to 2, 3 to 3, 5 to 4)
private val ACCENT_ANGLES = listOf(-35f, 0f, 35f)

private val IconSize = 96.dp
private const val ICON_START_SCALE = 0.85f
private val RiseDistance = 8.dp
private val TileWidth = 36.dp
private val TileTravel = 96.dp
private const val TILE_TILT_DEGREES = 16f
private val AccentRoom = 28.dp
private val AccentGap = 8.dp
private val AccentLength = 10.dp
private val AccentStroke = 2.5.dp
private val BarMaxWidth = 280.dp
private val BarHeight = 8.dp

@Preview(showBackground = true)
@Composable
private fun LoadingScreenPreview() {
    DominoTheme { LoadingScreen(ready = false) }
}
