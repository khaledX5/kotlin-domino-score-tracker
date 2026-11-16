package io.github.khaledx5.dominoscore.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.domain.history.LeaderboardEntry
import io.github.khaledx5.dominoscore.domain.history.Medal
import io.github.khaledx5.dominoscore.domain.history.PlayerCountHistory
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing
import java.math.BigDecimal
import java.text.NumberFormat

/** Totals and the leaderboard of one table size. */
@Immutable
data class StatisticsUi(val totalGames: Int, val totalPlayers: Int, val leaderboard: List<LeaderboardRowUi>)

@Immutable
data class LeaderboardRowUi(
    val profileId: Long,
    val position: Int,
    val medal: Medal?,
    val name: String,
    val wins: Int,
    val games: Int,
    val winRatePercent: Int,
    val averageScore: BigDecimal,
    val kingFinishes: Int,
    val koozFinishes: Int,
)

internal fun PlayerCountHistory.toStatisticsUi() = StatisticsUi(
    totalGames = totalGames,
    totalPlayers = totalPlayers,
    leaderboard = leaderboard.map(LeaderboardEntry::toUi),
)

private fun LeaderboardEntry.toUi() = LeaderboardRowUi(
    profileId = statistics.profile.id.value,
    position = position,
    medal = medal,
    name = statistics.profile.name.value,
    wins = statistics.wins,
    games = statistics.games,
    winRatePercent = statistics.winRatePercent,
    averageScore = statistics.averageScore,
    kingFinishes = statistics.kingFinishes,
    koozFinishes = statistics.koozFinishes,
)

@Composable
internal fun StatisticsList(statistics: StatisticsUi, playerCount: Int, modifier: Modifier = Modifier) {
    if (statistics.totalGames == 0) {
        EmptyCategory(
            text = pluralStringResource(R.plurals.stats_empty_category, playerCount, playerCount),
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        item(key = "totals") {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                TotalCard(
                    value = statistics.totalGames,
                    label = stringResource(R.string.stats_total_games),
                    modifier = Modifier.weight(1f),
                )
                TotalCard(
                    value = statistics.totalPlayers,
                    label = stringResource(R.string.stats_total_players),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item(key = "leaderboardTitle") {
            Text(
                text = stringResource(R.string.stats_leaderboard),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = Spacing.s),
            )
        }
        items(statistics.leaderboard, key = { it.profileId }) { row -> LeaderboardRow(row) }
    }
}

@Composable
private fun TotalCard(value: Int, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A medal (or the place number), the name with the wins, and the record in two short lines. */
@Composable
private fun LeaderboardRow(row: LeaderboardRowUi, modifier: Modifier = Modifier) {
    val gameColors = DominoThemeExtras.gameColors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (row.medal ==
                Medal.GOLD
            ) {
                gameColors.kingContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = if (row.medal == Medal.GOLD) BorderStroke(1.dp, gameColors.king) else null,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(
                text = placeMark(row),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 36.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(
                        pluralStringResource(R.plurals.stats_games, row.games, row.games),
                        stringResource(R.string.stats_win_rate, row.winRatePercent),
                        stringResource(R.string.stats_average, rememberOneDecimal(row.averageScore)),
                    ).joinToString(separator = stringResource(R.string.history_facts_separator)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.stats_titles, row.kingFinishes, row.koozFinishes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.stats_wins, row.wins),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun placeMark(row: LeaderboardRowUi): String = when (row.medal) {
    Medal.GOLD -> stringResource(R.string.stats_medal_gold)
    Medal.SILVER -> stringResource(R.string.stats_medal_silver)
    Medal.BRONZE -> stringResource(R.string.stats_medal_bronze)
    null -> stringResource(R.string.rank_number, row.position)
}

/** One decimal in the app language, so 103.7 reads as ١٠٣٫٧ in Arabic. */
@Composable
private fun rememberOneDecimal(value: BigDecimal): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(value, locale) {
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.format(value)
    }
}
