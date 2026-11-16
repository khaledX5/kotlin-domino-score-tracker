package io.github.khaledx5.dominoscore.ui.startup

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import io.github.khaledx5.dominoscore.ui.theme.Motion

/**
 * A teal bar that sweeps while startup is running and fills once it is ready. The app cannot know
 * how far a database open has got, so the bar never shows a percentage it would have to make up.
 * It sweeps from the start side, so it runs right to left in Arabic.
 */
@Composable
internal fun LoadingBar(ready: Boolean, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val barColor = MaterialTheme.colorScheme.primary
    val mirrored = LocalLayoutDirection.current == LayoutDirection.Rtl
    val sweep = rememberInfiniteTransition(label = "loadingSweep").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SWEEP_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "loadingSweepPosition",
    )
    val fill = animateFloatAsState(
        targetValue = if (ready) 1f else 0f,
        animationSpec = tween(Motion.QUICK_MS),
        label = "loadingFill",
    )
    val semantics = if (ready) Modifier.progressSemantics(1f) else Modifier.progressSemantics()

    Canvas(modifier = modifier.then(semantics)) {
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(color = trackColor, cornerRadius = corner)
        // Read in the draw phase only: the sweep redraws the bar, it never recomposes it.
        val head = lerp(-SEGMENT, 1f, sweep.value)
        val start = lerp(head.coerceIn(0f, 1f), 0f, fill.value)
        val end = lerp((head + SEGMENT).coerceIn(0f, 1f), 1f, fill.value)
        if (end > start) {
            scale(scaleX = if (mirrored) -1f else 1f, scaleY = 1f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(start * size.width, 0f),
                    size = Size((end - start) * size.width, size.height),
                    cornerRadius = corner,
                )
            }
        }
    }
}

/** Share of the track the moving segment covers. */
private const val SEGMENT = 0.35f
private const val SWEEP_MS = 1_400
