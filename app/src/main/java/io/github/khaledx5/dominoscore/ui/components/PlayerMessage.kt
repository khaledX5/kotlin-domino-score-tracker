package io.github.khaledx5.dominoscore.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.khaledx5.dominoscore.domain.messages.MessageTone
import io.github.khaledx5.dominoscore.domain.messages.RankingMessage
import io.github.khaledx5.dominoscore.ui.theme.CairoFontFamily
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Motion
import io.github.khaledx5.dominoscore.ui.theme.Spacing

/** The line a player shows beside their badge. [id] is the library id, so a new line is a new animation. */
@Immutable
data class PlayerMessageUi(val id: String, val text: String, val tone: MessageTone)

/**
 * The player's latest line, if it still fits: a KING line only while they are KING, a KOOZ line
 * only while they are KOOZ. Neutral lines (special events, general jokes) stay until the next one.
 */
fun RankingMessage.toUiFor(isKing: Boolean, isKooz: Boolean): PlayerMessageUi? {
    val tone = category.tone
    val fits = when (tone) {
        MessageTone.KING -> isKing
        MessageTone.KOOZ -> isKooz
        MessageTone.NEUTRAL -> true
    }
    return if (fits) PlayerMessageUi(id, text, tone) else null
}

/**
 * Shows [message] in a small pill: gold for KING, coral for KOOZ, quiet for the rest. Smaller than
 * the player's name and wrapping on narrow screens. A new line fades and slides in over about a
 * quarter of a second, keyed on its id, so recompositions never replay it.
 */
@Composable
fun PlayerMessageLine(message: PlayerMessageUi?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = message,
        contentKey = { it?.id },
        transitionSpec = {
            (fadeIn(tween(Motion.QUICK_MS)) + slideInVertically(tween(Motion.QUICK_MS)) { it / 2 })
                .togetherWith(fadeOut(tween(Motion.MESSAGE_OUT_MS)))
                .using(SizeTransform(clip = false))
        },
        modifier = modifier,
        label = "playerMessage",
    ) { shown ->
        if (shown == null) {
            Box(Modifier)
        } else {
            MessagePill(shown)
        }
    }
}

@Composable
private fun MessagePill(message: PlayerMessageUi, modifier: Modifier = Modifier) {
    val gameColors = DominoThemeExtras.gameColors
    val (container, accent) = when (message.tone) {
        MessageTone.KING -> gameColors.kingContainer to gameColors.king
        MessageTone.KOOZ -> gameColors.koozContainer to gameColors.kooz
        MessageTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.outline
    }
    // The faint outline keeps the pill visible on a card that is already tinted, like the KING result card.
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
        border = BorderStroke(1.dp, accent.copy(alpha = OUTLINE_ALPHA)),
    ) {
        Text(
            text = message.text,
            // The library is Egyptian Arabic in every app language, so it always uses the Arabic face.
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = CairoFontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs / 2),
        )
    }
}

private const val OUTLINE_ALPHA = 0.45f

@Preview(showBackground = true)
@Composable
private fun PlayerMessagePreview() {
    DominoTheme {
        PlayerMessageLine(PlayerMessageUi("first_time_king_001", "ركز عشان مش دايمة 😂", MessageTone.KING))
    }
}
