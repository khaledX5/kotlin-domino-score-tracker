package io.github.khaledx5.dominoscore.ui.summary

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.components.ConfettiBurst
import io.github.khaledx5.dominoscore.ui.components.PlayerMessageLine
import io.github.khaledx5.dominoscore.ui.components.StandingRow
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing

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
    LaunchedEffect(state) {
        val current = state
        if (current == SummaryUiState.Unavailable) currentOnClose()
        if (current is SummaryUiState.Ready && current.replayStarted) currentOnOpenGame()
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
) {
    val gameColors = DominoThemeExtras.gameColors
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
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.primaryButtonHeight),
        ) {
            Text(stringResource(R.string.home_new_game), style = MaterialTheme.typography.titleMedium)
        }
    }
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
