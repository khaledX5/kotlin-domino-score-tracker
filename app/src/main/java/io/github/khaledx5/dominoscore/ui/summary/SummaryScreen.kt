package io.github.khaledx5.dominoscore.ui.summary

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.share.CardTitle
import io.github.khaledx5.dominoscore.share.ResultCard
import io.github.khaledx5.dominoscore.share.ResultCardRow
import io.github.khaledx5.dominoscore.share.shareImageIntent
import io.github.khaledx5.dominoscore.ui.components.ConfettiBurst
import io.github.khaledx5.dominoscore.ui.components.PlayerMessageLine
import io.github.khaledx5.dominoscore.ui.components.StandingRow
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SummaryScreen(
    onClose: () -> Unit,
    onOpenGame: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    val currentOnOpenGame by rememberUpdatedState(onOpenGame)
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.share_chooser_title)
    val failedMessage = stringResource(R.string.share_failed)
    LaunchedEffect(state) {
        val current = state
        if (current == SummaryUiState.Unavailable) currentOnClose()
        if (current is SummaryUiState.Ready && current.replayStarted) currentOnOpenGame()
        if (current is SummaryUiState.Ready && current.pendingShare != null) {
            context.startActivity(shareImageIntent(context, current.pendingShare, chooserTitle))
            viewModel.onShareHandled()
        }
        if (current is SummaryUiState.Ready && current.shareFailed) {
            Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show()
            viewModel.onShareHandled()
        }
    }
    when (val current = state) {
        SummaryUiState.Loading, SummaryUiState.Unavailable -> Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is SummaryUiState.Ready -> SummaryContent(
            summary = current,
            onClose = onClose,
            onPlayAgain = viewModel::playAgain,
            onNewGame = onNewGame,
            modifier = modifier,
            onShare = viewModel::share,
        )
    }
}

/**
 * The winner screen: the trophy bounces in with the winner's name and score and one burst of
 * confetti, then the final ranking with KING / KOOZ and everyone's last line, and the way out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryContent(
    summary: SummaryUiState.Ready,
    onClose: () -> Unit,
    onPlayAgain: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: (ResultCard) -> Unit = {},
) {
    val gameColors = DominoThemeExtras.gameColors
    val card = rememberResultCard(summary)
    Box(modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.summary_close),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onShare(card) }, enabled = !summary.isSharing) {
                            Icon(
                                painter = painterResource(R.drawable.ic_share),
                                contentDescription = stringResource(R.string.share_result),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            bottomBar = { SummaryActions(onPlayAgain = onPlayAgain, onNewGame = onNewGame) },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                item(key = "winner") { WinnerHeader(winner = summary.winner) }
                item(key = "ranking") { FinalRanking(standings = summary.standings) }
            }
        }
        ConfettiBurst(
            colors = listOf(
                gameColors.king,
                gameColors.add,
                gameColors.penalty,
                MaterialTheme.colorScheme.primaryContainer,
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun WinnerHeader(winner: StandingUi, modifier: Modifier = Modifier) {
    val trophyScale = remember { Animatable(TROPHY_START_SCALE) }
    LaunchedEffect(Unit) {
        trophyScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
    }
    Column(
        modifier = modifier.fillMaxWidth().padding(top = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(
            text = stringResource(R.string.summary_trophy),
            fontSize = TROPHY_SIZE.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = trophyScale.value
                scaleY = trophyScale.value
            },
        )
        Text(
            text = stringResource(R.string.summary_wins, winner.name),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = pluralStringResource(R.plurals.points_value, winner.score, winner.score),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FinalRanking(standings: List<StandingUi>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                text = stringResource(R.string.summary_final_ranking),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.s),
            )
            standings.forEach { standing ->
                StandingRow(standing)
                PlayerMessageLine(standing.message, modifier = Modifier.padding(start = Spacing.xxl))
            }
        }
    }
}

@Composable
private fun SummaryActions(onPlayAgain: () -> Unit, onNewGame: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = Spacing.xl, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Button(
            onClick = onPlayAgain,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.primaryButtonHeight),
        ) {
            Text(stringResource(R.string.summary_play_again), style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(
            onClick = onNewGame,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.primaryButtonHeight),
        ) {
            Text(stringResource(R.string.home_new_game), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** The shared image's text, in the app language and layout direction. */
@Composable
private fun rememberResultCard(summary: SummaryUiState.Ready): ResultCard {
    val locale = LocalConfiguration.current.locales[0]
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val date = summary.completedAt?.let { completedAt ->
        remember(completedAt, locale) {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault())
                .format(completedAt)
        }
    }
    val target = stringResource(R.string.share_target, summary.replayTarget)
    val rows = summary.standings.map { standing ->
        ResultCardRow(
            mark = when {
                standing.isKing -> stringResource(R.string.rank_king_mark)
                standing.isKooz -> stringResource(R.string.rank_kooz_mark)
                else -> stringResource(R.string.rank_number, standing.rank)
            },
            name = standing.name,
            score = standing.score.toString(),
            title = when {
                standing.isKing -> CardTitle.KING
                standing.isKooz -> CardTitle.KOOZ
                else -> null
            },
            titleLabel = when {
                standing.isKing -> stringResource(R.string.rank_king_badge)
                standing.isKooz -> stringResource(R.string.rank_kooz_badge)
                else -> null
            },
        )
    }
    return ResultCard(
        brand = stringResource(R.string.app_name).uppercase(locale),
        trophy = stringResource(R.string.summary_trophy),
        winnerLine = stringResource(R.string.summary_wins, summary.winner.name),
        scoreLine = pluralStringResource(R.plurals.points_value, summary.winner.score, summary.winner.score),
        rows = rows,
        footer = listOfNotNull(date, target).joinToString(separator = stringResource(R.string.history_facts_separator)),
        rtl = rtl,
    )
}

private const val TROPHY_START_SCALE = 0.3f
private const val TROPHY_SIZE = 96

@Preview
@Composable
private fun SummaryPreview() {
    val omar = StandingUi(1, rank = 1, name = "Omar", score = 160, colorIndex = 0, qualified = true, isKing = true)
    DominoTheme {
        SummaryContent(
            summary = SummaryUiState.Ready(winner = omar, standings = listOf(omar)),
            onClose = {},
            onPlayAgain = {},
            onNewGame = {},
        )
    }
}
