package io.github.khaledx5.dominoscore.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import io.github.khaledx5.dominoscore.ui.theme.Motion
import io.github.khaledx5.dominoscore.ui.theme.rememberReducedMotion
import kotlin.random.Random

private data class Piece(
    val startX: Float,
    val drift: Float,
    val delay: Float,
    val fallSpeed: Float,
    val spin: Float,
    val width: Float,
    val color: Color,
)

/**
 * One burst of confetti falling over the screen for about [Motion.CELEBRATION_MS], then gone.
 * Drawn on a single Canvas, so it never takes touches. Skipped entirely when animations are off.
 */
@Composable
fun ConfettiBurst(colors: List<Color>, modifier: Modifier = Modifier, pieces: Int = PIECES) {
    if (rememberReducedMotion()) return
    val progress = remember { Animatable(0f) }
    val confetti = remember(colors, pieces) {
        val random = Random.Default
        List(pieces) {
            Piece(
                startX = random.nextFloat(),
                drift = (random.nextFloat() - 0.5f) * MAX_DRIFT,
                delay = random.nextFloat() * MAX_DELAY,
                fallSpeed = MIN_SPEED + random.nextFloat() * (1f - MIN_SPEED),
                spin = (random.nextFloat() - 0.5f) * MAX_SPIN,
                width = MIN_WIDTH + random.nextFloat() * MIN_WIDTH,
                color = colors[it % colors.size],
            )
        }
    }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(Motion.CELEBRATION_MS, easing = LinearEasing))
    }
    Canvas(modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        confetti.forEach { piece ->
            val local = ((t - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val x = (piece.startX + piece.drift * local) * size.width
            val y = -piece.width + local * piece.fallSpeed * (size.height + piece.width * 2)
            val alpha = if (local > FADE_FROM) (1f - local) / (1f - FADE_FROM) else 1f
            rotate(degrees = piece.spin * local, pivot = Offset(x, y)) {
                drawRect(
                    color = piece.color.copy(alpha = alpha),
                    topLeft = Offset(x, y),
                    size = Size(piece.width, piece.width * PIECE_RATIO),
                )
            }
        }
    }
}

private const val PIECES = 90
private const val MAX_DRIFT = 0.25f
private const val MAX_DELAY = 0.3f
private const val MIN_SPEED = 0.6f
private const val MAX_SPIN = 720f
private const val MIN_WIDTH = 10f
private const val PIECE_RATIO = 0.5f
private const val FADE_FROM = 0.8f
