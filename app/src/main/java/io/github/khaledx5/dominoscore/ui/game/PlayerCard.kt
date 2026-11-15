package io.github.khaledx5.dominoscore.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.components.AnimatedScore
import io.github.khaledx5.dominoscore.ui.components.KingBadge
import io.github.khaledx5.dominoscore.ui.components.KoozBadge
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing

/** A player's card on the game screen: who they are, where they stand and their score. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerCard(player: StandingUi, modifier: Modifier = Modifier, actions: @Composable ColumnScope.() -> Unit = {}) {
    val gameColors = DominoThemeExtras.gameColors
    val border by animateColorAsState(
        targetValue = if (player.isKing) gameColors.king else Color.Transparent,
        label = "kingCardBorder",
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, border),
    ) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                PlayerAvatar(
                    name = player.name,
                    color = gameColors.players[player.colorIndex % gameColors.players.size],
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        if (!player.isKing && !player.isKooz) RankChip(rank = player.rank)
                        if (player.isKing) KingBadge()
                        if (player.isKooz) KoozBadge()
                    }
                }
                AnimatedScore(score = player.score)
            }
            actions()
        }
    }
}

@Composable
private fun PlayerAvatar(name: String, color: Color, modifier: Modifier = Modifier) {
    val textColor = if (color.luminance() > AVATAR_LIGHT_THRESHOLD) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = modifier.size(48.dp),
        shape = MaterialTheme.shapes.medium,
        color = color,
        contentColor = textColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun RankChip(rank: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = stringResource(R.string.rank_number, rank),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs / 2),
        )
    }
}

/** The two score buttons under each card. Colour and sign tell them apart, not only the word. */
@Composable
fun ScoreButtons(onAdd: () -> Unit, onPenalty: () -> Unit, modifier: Modifier = Modifier) {
    val gameColors = DominoThemeExtras.gameColors
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        ScoreButton(
            label = stringResource(R.string.game_add),
            iconRes = R.drawable.ic_add,
            accent = gameColors.add,
            container = gameColors.addContainer,
            onClick = onAdd,
            modifier = Modifier.weight(1f),
        )
        ScoreButton(
            label = stringResource(R.string.game_penalty),
            iconRes = R.drawable.ic_remove,
            accent = gameColors.penalty,
            container = gameColors.penaltyContainer,
            onClick = onPenalty,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ScoreButton(
    label: String,
    @DrawableRes iconRes: Int,
    accent: Color,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium, color = container) {
        Row(
            modifier = Modifier.padding(vertical = Spacing.m),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = Spacing.xs),
            )
        }
    }
}

private const val AVATAR_LIGHT_THRESHOLD = 0.4f

@Preview
@Composable
private fun PlayerCardPreview() {
    DominoTheme {
        PlayerCard(
            StandingUi(
                playerId = 1,
                rank = 1,
                name = "Ahmed",
                score = 160,
                colorIndex = 0,
                qualified = true,
                isKing = true,
            ),
        )
    }
}
