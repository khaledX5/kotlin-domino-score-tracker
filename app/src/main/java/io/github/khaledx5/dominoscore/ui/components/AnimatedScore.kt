package io.github.khaledx5.dominoscore.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Motion
import io.github.khaledx5.dominoscore.ui.theme.forNumbers
import io.github.khaledx5.dominoscore.ui.theme.rememberReducedMotion
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A score that counts to its new value, pops slightly and shows a floating +N (teal) or -N
 * (coral) label. It reacts to the score itself, so nothing waits for the animation and the first
 * time a score appears it is shown without any effect.
 */
@Composable
fun AnimatedScore(score: Int, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.displaySmall) {
    val reducedMotion = rememberReducedMotion()
    val shown by animateIntAsState(targetValue = score, animationSpec = tween(Motion.QUICK_MS), label = "scoreCount")
    val scale = remember { Animatable(1f) }
    var previous by remember { mutableStateOf<Int?>(null) }
    var showDelta by remember { mutableStateOf(false) }
    var lastDelta by remember { mutableIntStateOf(0) }

    LaunchedEffect(score) {
        val before = previous
        previous = score
        if (before == null || before == score) return@LaunchedEffect
        lastDelta = score - before
        showDelta = !reducedMotion
        launch {
            scale.snapTo(POP_SCALE)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        delay(Motion.FLOATING_LABEL_MS.toLong())
        showDelta = false
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = shown.toString(),
            style = style.forNumbers(),
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
        AnimatedVisibility(
            visible = showDelta,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = (-18).dp),
        ) {
            FloatingDelta(delta = lastDelta)
        }
    }
}

@Composable
private fun FloatingDelta(delta: Int, modifier: Modifier = Modifier) {
    val gameColors = DominoThemeExtras.gameColors
    val gained = delta >= 0
    Text(
        text = stringResource(if (gained) R.string.score_delta_add else R.string.score_delta_penalty, abs(delta)),
        style = MaterialTheme.typography.titleMedium.forNumbers(),
        color = if (gained) gameColors.add else gameColors.penalty,
        modifier = modifier,
    )
}

private const val POP_SCALE = 1.15f
