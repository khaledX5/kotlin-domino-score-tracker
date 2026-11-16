package io.github.khaledx5.dominoscore.ui.startup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.khaledx5.dominoscore.ui.theme.rememberReducedMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The loading screen's entrance, about 1.6 s from the first frame: the icon, the title and the
 * tagline fade in, three dominoes slide in one after another, then the teal accents open around them.
 * Every part is a progress from 0 (not shown) to 1 (settled); the screen turns those into alpha,
 * offset and rotation in the draw phase, so the animation never recomposes the screen.
 */
@Stable
internal class LoadingIntroState(val settled: Boolean) {
    private val start = if (settled) 1f else 0f

    val icon = Animatable(start)
    val title = Animatable(start)
    val tagline = Animatable(start)
    val tiles = List(TILE_COUNT) { Animatable(start) }
    val accents = Animatable(start)
    val progress = Animatable(start)

    suspend fun play() = coroutineScope {
        launch { icon.animateTo(1f, tween(ICON_MS, easing = FastOutSlowInEasing)) }
        launch { title.animateTo(1f, tween(FADE_MS, delayMillis = TITLE_DELAY_MS, easing = FastOutSlowInEasing)) }
        launch { tagline.animateTo(1f, tween(FADE_MS, delayMillis = TAGLINE_DELAY_MS, easing = FastOutSlowInEasing)) }
        launch { progress.animateTo(1f, tween(FADE_MS, delayMillis = FIRST_TILE_DELAY_MS.toInt())) }
        tiles.forEachIndexed { index, tile ->
            launch {
                delay(FIRST_TILE_DELAY_MS + index * TILE_STAGGER_MS)
                tile.animateTo(1f, TileSpring)
            }
        }
        launch {
            delay(ACCENTS_DELAY_MS)
            accents.animateTo(1f, tween(ACCENTS_MS, easing = FastOutSlowInEasing))
        }
    }

    companion object {
        const val TILE_COUNT = 3
        private const val ICON_MS = 350
        private const val FADE_MS = 350
        private const val TITLE_DELAY_MS = 120
        private const val TAGLINE_DELAY_MS = 240
        private const val FIRST_TILE_DELAY_MS = 450L
        private const val TILE_STAGGER_MS = 250L
        private const val ACCENTS_DELAY_MS = 1_250L
        private const val ACCENTS_MS = 350

        /** Settles with a small overshoot, never a bounce. */
        private val TileSpring = spring<Float>(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
    }
}

/**
 * The entrance plays once per visit. A rotation in the middle shows the settled screen instead of
 * starting over, and with animations switched off in the system settings it is settled from the start.
 */
@Composable
internal fun rememberLoadingIntroState(): LoadingIntroState {
    val reducedMotion = rememberReducedMotion()
    var played by rememberSaveable { mutableStateOf(false) }
    val intro = remember { LoadingIntroState(settled = played || reducedMotion) }
    LaunchedEffect(intro) {
        if (!intro.settled) {
            played = true
            intro.play()
        }
    }
    return intro
}
