package io.github.khaledx5.dominoscore.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.khaledx5.dominoscore.ui.theme.Motion
import io.github.khaledx5.dominoscore.ui.theme.rememberReducedMotion
import kotlinx.coroutines.launch

/**
 * False while a card is being composed for the first time, true from the next frame on.
 * A badge that appears after that is a real title change; one that is there from the start
 * (opening the screen, scrolling back to a card) shows without any effect.
 */
@Composable
fun rememberSettled(): Boolean {
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { settled = true }
    return settled
}

/** 👑 The new KING's badge pops in with a bounce, then a short shine sweeps across it. */
@Composable
fun CrownEntrance(animate: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val scale = remember { Animatable(if (animate) CROWN_START_SCALE else 1f) }
    val shine = remember { Animatable(if (animate && !reducedMotion) 0f else SHINE_DONE) }

    LaunchedEffect(Unit) {
        if (!animate) return@LaunchedEffect
        launch {
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
        }
        if (!reducedMotion) shine.animateTo(SHINE_DONE, tween(Motion.SHIMMER_MS))
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .drawWithContent {
                drawContent()
                val progress = shine.value
                if (progress < SHINE_DONE) {
                    val band = size.width * SHINE_WIDTH
                    val x = -band + (size.width + band) * progress
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = SHINE_ALPHA),
                                Color.Transparent,
                            ),
                            start = Offset(x, 0f),
                            end = Offset(x + band, size.height),
                        ),
                    )
                }
            },
    ) {
        content()
    }
}

/** 🪣 The new KOOZ's bucket drops in from above and lands with a small bounce. */
@Composable
fun BucketDrop(animate: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dropFrom = with(LocalDensity.current) { BUCKET_DROP_HEIGHT.toPx() }
    val offset = remember { Animatable(if (animate) -dropFrom else 0f) }
    val alpha = remember { Animatable(if (animate) 0f else 1f) }

    LaunchedEffect(Unit) {
        if (!animate) return@LaunchedEffect
        launch { alpha.animateTo(1f, tween(Motion.QUICK_MS)) }
        offset.animateTo(
            0f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            translationY = offset.value
            this.alpha = alpha.value
        },
    ) {
        content()
    }
}

private const val CROWN_START_SCALE = 0.4f
private const val SHINE_DONE = 1f
private const val SHINE_WIDTH = 0.6f
private const val SHINE_ALPHA = 0.7f
private val BUCKET_DROP_HEIGHT = 20.dp
