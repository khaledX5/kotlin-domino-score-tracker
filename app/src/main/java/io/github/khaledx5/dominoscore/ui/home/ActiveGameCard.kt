package io.github.khaledx5.dominoscore.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.components.KingBadge
import io.github.khaledx5.dominoscore.ui.components.KoozBadge
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing

@Composable
fun ActiveGameCard(game: ActiveGameUi, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs)) {
                Text(
                    text = stringResource(R.string.home_active_game_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.home_active_game_details, game.roundNumber, game.target),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            game.standings.forEach { standing ->
                StandingRow(standing)
            }
        }
    }
}

/**
 * One line of the standings. The KING row gets a gold highlight that fades in and out as the
 * title moves between players; the badges appear and disappear with a short, quiet transition.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StandingRow(standing: StandingUi, modifier: Modifier = Modifier) {
    val gameColors = DominoThemeExtras.gameColors
    val highlight by animateColorAsState(
        targetValue = if (standing.isKing) gameColors.kingContainer else Color.Transparent,
        label = "kingHighlight",
    )
    val border by animateColorAsState(
        targetValue = if (standing.isKing) gameColors.king else Color.Transparent,
        label = "kingBorder",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = highlight,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(
                text = leadingMark(standing),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 28.dp),
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(gameColors.players[standing.colorIndex % gameColors.players.size], CircleShape),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = standing.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    AnimatedVisibility(visible = standing.isKing, enter = BadgeEnter, exit = BadgeExit) { KingBadge() }
                    AnimatedVisibility(visible = standing.isKooz, enter = BadgeEnter, exit = BadgeExit) { KoozBadge() }
                    if (standing.qualified) {
                        QualifiedBadge()
                    }
                }
            }
            Text(
                text = standing.score.toString(),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

private val BadgeEnter = fadeIn() + expandVertically()
private val BadgeExit = fadeOut() + shrinkVertically()

@Composable
private fun leadingMark(standing: StandingUi): String = when {
    standing.isKing -> stringResource(R.string.rank_king_mark)
    standing.isKooz -> stringResource(R.string.rank_kooz_mark)
    else -> stringResource(R.string.rank_number, standing.rank)
}

@Composable
private fun QualifiedBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.qualified),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .background(DominoThemeExtras.gameColors.qualifiedContainer, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = Spacing.s, vertical = Spacing.xs / 2),
    )
}

@Preview
@Composable
private fun ActiveGameCardPreview() {
    DominoTheme {
        ActiveGameCard(
            ActiveGameUi(
                roundNumber = 3,
                target = 150,
                standings = listOf(
                    StandingUi(
                        1,
                        rank = 1,
                        name = "Ahmed",
                        score = 160,
                        colorIndex = 0,
                        qualified = true,
                        isKing = true,
                    ),
                    StandingUi(2, rank = 2, name = "Khaled", score = 95, colorIndex = 1, qualified = false),
                    StandingUi(3, rank = 3, name = "Mohamed", score = 40, colorIndex = 2, qualified = false),
                    StandingUi(
                        4,
                        rank = 4,
                        name = "Ali",
                        score = -15,
                        colorIndex = 3,
                        qualified = false,
                        isKooz = true,
                    ),
                ),
            ),
        )
    }
}
