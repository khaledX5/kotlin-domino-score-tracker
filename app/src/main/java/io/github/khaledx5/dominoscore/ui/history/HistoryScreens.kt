package io.github.khaledx5.dominoscore.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.ui.components.StandingRow
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Motion
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
    HistoryContent(
        state = state,
        onBack = onBack,
        onSelectPlayerCount = viewModel::selectPlayerCount,
        onSelectTab = viewModel::selectTab,
        onOpenGame = onOpenGame,
        modifier = modifier,
    )
}

/**
 * Completed games split by table size: 2, 3 and 4 players never mix. History shows each game with
 * the confirmed winner and its frozen final ranking; Statistics shows the totals and leaderboard
 * of that size only.
 */
@Composable
fun HistoryContent(
    state: HistoryUiState,
    onBack: () -> Unit,
    onSelectPlayerCount: (Int) -> Unit,
    onOpenGame: (gameId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onSelectTab: (HistoryTab) -> Unit = {},
) {
    ScreenScaffold(title = stringResource(R.string.history_title), onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PlayerCountSelector(
                selected = state.playerCount,
                onSelect = onSelectPlayerCount,
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.s),
            )
            HistoryTabs(selected = state.tab, onSelect = onSelectTab)
            AnimatedContent(
                targetState = state,
                contentKey = { Triple(it.playerCount, it.tab, it.content is HistoryContentState.Ready) },
                transitionSpec = { fadeIn(tween(Motion.QUICK_MS)) togetherWith fadeOut(tween(Motion.QUICK_MS)) },
                label = "historyCategory",
                modifier = Modifier.weight(1f),
            ) { shown ->
                when (val content = shown.content) {
                    // Reading one category takes a moment; a blank frame is calmer than a spinner flash.
                    HistoryContentState.Loading -> Box(Modifier.fillMaxSize())

                    is HistoryContentState.Ready -> when (shown.tab) {
                        HistoryTab.HISTORY -> GameList(
                            games = content.games,
                            playerCount = shown.playerCount,
                            onOpenGame = onOpenGame,
                        )

                        HistoryTab.STATISTICS -> StatisticsList(
                            statistics = content.statistics,
                            playerCount = shown.playerCount,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerCountSelector(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val counts = Game.PLAYER_COUNT.toList()
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        counts.forEachIndexed { index, count ->
            SegmentedButton(
                selected = count == selected,
                onClick = { onSelect(count) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = counts.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                ),
                icon = {},
                label = { Text(pluralStringResource(R.plurals.history_player_count, count, count)) },
            )
        }
    }
}

@Composable
private fun HistoryTabs(selected: HistoryTab, onSelect: (HistoryTab) -> Unit) {
    PrimaryTabRow(
        selectedTabIndex = selected.ordinal,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        HistoryTab.entries.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        text = stringResource(
                            when (tab) {
                                HistoryTab.HISTORY -> R.string.history_tab_history
                                HistoryTab.STATISTICS -> R.string.history_tab_statistics
                            },
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
            )
        }
    }
}

@Composable
private fun GameList(games: List<CompletedGameUi>, playerCount: Int, onOpenGame: (gameId: Long) -> Unit) {
    if (games.isEmpty()) {
        EmptyCategory(text = pluralStringResource(R.plurals.history_empty_category, playerCount, playerCount))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        items(games, key = { it.gameId }) { game ->
            CompletedGameCard(game = game, onClick = { onOpenGame(game.gameId) })
        }
    }
}

@Composable
internal fun EmptyCategory(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CompletedGameCard(game: CompletedGameUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Text(
                    text = stringResource(R.string.history_winner, game.winnerName),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = game.winnerScore.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = DominoThemeExtras.gameColors.king,
                )
            }
            Text(
                text = gameFacts(game),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            game.standings.forEach { StandingRow(it) }
        }
    }
}

/** "16 Nov 2026, 21:40 · 5 rounds · 35 min", in the app language. */
@Composable
private fun gameFacts(game: CompletedGameUi): String = listOf(
    rememberFormattedDate(game.completedAt),
    pluralStringResource(R.plurals.history_rounds, game.rounds, game.rounds),
    durationText(game.durationMinutes),
).joinToString(separator = stringResource(R.string.history_facts_separator))

@Composable
private fun durationText(minutes: Long): String {
    val hours = (minutes / MINUTES_PER_HOUR).toInt()
    val rest = (minutes % MINUTES_PER_HOUR).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.duration_under_a_minute)
        hours == 0 -> stringResource(R.string.duration_minutes, rest)
        else -> stringResource(R.string.duration_hours_minutes, hours, rest)
    }
}

private const val MINUTES_PER_HOUR = 60

@Composable
private fun GameHeadline(game: PastGameUi) {
    // Stacked, so a long winner name is never cut off by the date.
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs / 2)) {
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
