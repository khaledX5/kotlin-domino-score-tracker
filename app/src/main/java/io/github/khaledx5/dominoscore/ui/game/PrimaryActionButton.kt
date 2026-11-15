package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.domain.rules.PrimaryAction
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Motion
import io.github.khaledx5.dominoscore.ui.theme.Spacing

/**
 * The one main button of the game screen. Teal "End round" while nobody reached the target,
 * gold "End game" as soon as anyone did; the change animates so the table notices it.
 */
@Composable
fun PrimaryActionButton(action: PrimaryAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val container by animateColorAsState(
        targetValue = when (action) {
            PrimaryAction.END_ROUND -> MaterialTheme.colorScheme.primary
            PrimaryAction.END_GAME -> DominoThemeExtras.gameColors.king
        },
        animationSpec = tween(Motion.RANKING_MS),
        label = "primaryActionColor",
    )
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = Spacing.xl, vertical = Spacing.l)
            .height(Spacing.primaryButtonHeight),
    ) {
        AnimatedContent(
            targetState = action,
            transitionSpec = {
                (fadeIn(tween(Motion.QUICK_MS)) + slideInVertically { it / 2 })
                    .togetherWith(fadeOut(tween(Motion.QUICK_MS)) + slideOutVertically { -it / 2 })
            },
            label = "primaryActionLabel",
        ) { current ->
            Text(
                text = stringResource(
                    when (current) {
                        PrimaryAction.END_ROUND -> R.string.game_end_round
                        PrimaryAction.END_GAME -> R.string.game_end_game
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
