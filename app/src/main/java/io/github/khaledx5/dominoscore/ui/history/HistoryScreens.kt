package io.github.khaledx5.dominoscore.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.components.StandingRow
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenGame: (gameId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(state = state, onBack = onBack, onOpenGame = onOpenGame, modifier = modifier)
}

/** Past games, newest first: when, the target, who won (or that it was left unfinished) and the final scores. */
@Composable
fun HistoryContent(
    state: HistoryUiState,
    onBack: () -> Unit,
    onOpenGame: (gameId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(title = stringResource(R.string.history_title), onBack = onBack, modifier = modifier) { padding ->
        when (state) {
            HistoryUiState.Loading -> Loading(padding)

            is HistoryUiState.Ready -> if (state.games.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    items(state.games, key = { it.gameId }) { game ->
                        PastGameCard(game = game, onClick = { onOpenGame(game.gameId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PastGameCard(game: PastGameUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            GameHeadline(game)
            Text(
                text = game.standings.joinToString(separator = "  ·  ") { "${it.name} ${it.score}" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GameHeadline(game: PastGameUi) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = game.winnerName?.let { stringResource(R.string.history_winner, it) }
                ?: stringResource(R.string.history_abandoned),
            style = MaterialTheme.typography.titleMedium,
            color = if (game.winnerName == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.history_date_target, rememberFormattedDate(game.startedAt), game.target),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun GameDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(state) {
        if (state == GameDetailUiState.Missing) currentOnBack()
    }
    GameDetailContent(state = state, onBack = onBack, modifier = modifier)
}

/** One past game: the final standings, then every round with each player's points and running total. */
@Composable
fun GameDetailContent(state: GameDetailUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ScreenScaffold(title = stringResource(R.string.detail_title), onBack = onBack, modifier = modifier) { padding ->
        when (state) {
            GameDetailUiState.Loading, GameDetailUiState.Missing -> Loading(padding)

            is GameDetailUiState.Ready -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                item(key = "final") {
                    SectionCard {
                        GameHeadline(state.game)
                        state.game.standings.forEach { StandingRow(it) }
                    }
                }
                items(state.rounds, key = { it.number }) { round -> RoundCard(round) }
            }
        }
    }
}

@Composable
private fun RoundCard(round: RoundRowUi, modifier: Modifier = Modifier) {
    val gameColors = DominoThemeExtras.gameColors
    SectionCard(modifier) {
        Text(
            text = stringResource(
                if (round.completed) R.string.game_round else R.string.detail_round_unfinished,
                round.number,
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        round.cells.forEach { cell ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                Text(
                    text = cell.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        if (cell.points < 0) R.string.score_delta_penalty else R.string.score_delta_add,
                        kotlin.math.abs(cell.points),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        cell.points > 0 -> gameColors.add
                        cell.points < 0 -> gameColors.penalty
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(text = cell.total.toString(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        content = content,
    )
}

@Composable
private fun Loading(padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** The start time in the phone's time zone and the app language. */
@Composable
private fun rememberFormattedDate(instant: Instant): String {
    // Follows the app language picked in settings, and recomposes when it changes.
    val locale = LocalConfiguration.current.locales[0]
    return remember(instant, locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
}
