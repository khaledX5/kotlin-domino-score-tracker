package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.domain.history.ScoreMove
import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing
import kotlin.math.abs

/** One Add or Penalty in the move history. The name is today's name, so a rename shows everywhere. */
@Immutable
data class MoveUi(
    val id: Long,
    val playerName: String,
    val colorIndex: Int,
    val roundNumber: Int,
    val points: Int,
    val total: Int,
)

internal fun ScoreMove.toUi(players: List<Player>): MoveUi? = players.firstOrNull { it.id == playerId }?.let { player ->
    MoveUi(
        id = id,
        playerName = player.name.value,
        colorIndex = player.colorIndex,
        roundNumber = roundNumber,
        points = points,
        total = newScore,
    )
}

/** The latest moves of the game, newest on top, under the live ranking. */
@Composable
fun RecentMovesCard(
    moves: List<MoveUi>,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                text = stringResource(R.string.game_history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.s),
            )
            if (moves.isEmpty()) {
                Text(
                    text = stringResource(R.string.game_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.s),
                )
            } else {
                moves.forEach { MoveRow(it) }
            }
            footer()
        }
    }
}

/** Who, in which round, how many points (teal for Add, coral for Penalty) and the total it left. */
@Composable
fun MoveRow(move: MoveUi, modifier: Modifier = Modifier, showRound: Boolean = true) {
    val gameColors = DominoThemeExtras.gameColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(gameColors.players[move.colorIndex % gameColors.players.size], CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = move.playerName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showRound) {
                Text(
                    text = stringResource(R.string.game_round, move.roundNumber),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(
                if (move.points < 0) R.string.score_delta_penalty else R.string.score_delta_add,
                abs(move.points),
            ),
            style = MaterialTheme.typography.labelLarge,
            color = if (move.points < 0) gameColors.penalty else gameColors.add,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = POINTS_WIDTH),
        )
        Text(
            text = stringResource(R.string.move_total, move.total),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = TOTAL_WIDTH),
        )
    }
}

/** Fixed columns, so points and totals line up from row to row. */
private val POINTS_WIDTH = 48.dp
private val TOTAL_WIDTH = 64.dp
