package io.github.khaledx5.dominoscore.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.khaledx5.dominoscore.R
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
            modifier = Modifier.padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(
                text = stringResource(R.string.home_active_game_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.home_active_game_details, game.roundNumber, game.target),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            game.standings.forEach { standing ->
                StandingRow(standing)
            }
        }
    }
}

@Composable
private fun StandingRow(standing: StandingUi, modifier: Modifier = Modifier) {
    val gameColors = DominoThemeExtras.gameColors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Text(
            text = stringResource(R.string.rank_number, standing.rank),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(gameColors.players[standing.colorIndex % gameColors.players.size], CircleShape),
        )
        Text(
            text = standing.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (standing.qualified) {
            Text(
                text = stringResource(R.string.qualified),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .background(gameColors.qualifiedContainer, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = Spacing.s, vertical = 2.dp),
            )
        }
        Text(
            text = standing.score.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
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
                    StandingUi(playerId = 1, rank = 1, name = "Omar", score = 160, colorIndex = 0, qualified = true),
                    StandingUi(playerId = 2, rank = 2, name = "Mona", score = 95, colorIndex = 1, qualified = false),
                    StandingUi(playerId = 3, rank = 2, name = "Ali", score = 95, colorIndex = 2, qualified = false),
                ),
            ),
        )
    }
}
