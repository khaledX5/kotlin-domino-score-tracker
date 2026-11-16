package io.github.khaledx5.dominoscore.ui.game

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent
import io.github.khaledx5.dominoscore.ui.theme.Motion
import io.github.khaledx5.dominoscore.ui.theme.Spacing
import io.github.khaledx5.dominoscore.ui.theme.rememberReducedMotion

/**
 * A short line at the top of the game for a moment like a tie or the final stretch. It is not a
 * dialog and takes no taps, so play goes on under it, and the screen reader announces it once.
 */
@Composable
fun ReactionLabel(reaction: GameSoundEvent?, modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    AnimatedContent(
        targetState = reaction,
        modifier = modifier,
        transitionSpec = {
            if (reducedMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                (fadeIn(tween(Motion.QUICK_MS)) + slideInVertically(tween(Motion.QUICK_MS)) { -it / 2 }) togetherWith
                    fadeOut(tween(Motion.QUICK_MS))
            }
        },
        label = "reaction",
    ) { event ->
        if (event != null) {
            // A plain box, not a Surface: a Surface would swallow taps meant for the cards under it.
            Box(
                modifier = Modifier
                    .shadow(Spacing.xs, MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.inverseSurface, MaterialTheme.shapes.large)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    text = stringResource(event.reaction),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
                )
            }
        }
    }
}

@get:StringRes
private val GameSoundEvent.reaction: Int
    get() = when (this) {
        GameSoundEvent.SECOND_QUALIFIED -> R.string.reaction_second_qualified
        GameSoundEvent.FINAL_STRETCH -> R.string.reaction_final_stretch
        GameSoundEvent.LAST_PLAYER_SCORES -> R.string.reaction_last_player_scores
        GameSoundEvent.EQUAL_SCORES -> R.string.reaction_equal_scores
        GameSoundEvent.LARGE_PENALTY -> R.string.reaction_large_penalty
    }
