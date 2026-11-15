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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
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
import io.github.khaledx5.dominoscore.ui.components.StandingRow
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing

@Composable
fun ActiveGameCard(game: ActiveGameUi, onContinue: () -> Unit, modifier: Modifier = Modifier) {
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
            Button(
                onClick = onContinue,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.primaryButtonHeight),
            ) {
                Text(stringResource(R.string.home_continue_game), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Preview
@Composable
private fun ActiveGameCardPreview() {
    DominoTheme {
        ActiveGameCard(
            onContinue = {},
            game = ActiveGameUi(
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
