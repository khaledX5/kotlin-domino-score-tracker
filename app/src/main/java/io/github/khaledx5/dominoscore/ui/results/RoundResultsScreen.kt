package io.github.khaledx5.dominoscore.ui.results

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.components.KingBadge
import io.github.khaledx5.dominoscore.ui.components.KoozBadge
import io.github.khaledx5.dominoscore.ui.components.PlayerMessageLine
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Motion
import io.github.khaledx5.dominoscore.ui.theme.Spacing
import io.github.khaledx5.dominoscore.ui.theme.forNumbers
import io.github.khaledx5.dominoscore.ui.theme.rememberReducedMotion
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun RoundResultsScreen(
    onContinue: () -> Unit,
    onGameClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoundResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnGameClose by rememberUpdatedState(onGameClose)

    LaunchedEffect(state) {
        if (state == RoundResultsUiState.NoActiveGame) currentOnGameClose()
    }

    when (val current = state) {
        RoundResultsUiState.Loading, RoundResultsUiState.NoActiveGame -> Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is RoundResultsUiState.Ready -> RoundResultsContent(
            results = current,
            onContinue = onContinue,
            modifier = modifier,
        )
    }
}

/**
 * The standings after a round: cards arrive one after another, each score counts up, and the
 * KING / KOOZ titles are revealed once the counting is done. With animations off it all shows at once.
 */
@Composable
fun RoundResultsContent(results: RoundResultsUiState.Ready, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Button(
                onClick = onContinue,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = Spacing.xl, vertical = Spacing.l)
                    .height(Spacing.primaryButtonHeight),
            ) {
                Text(stringResource(R.string.round_results_continue), style = MaterialTheme.typography.titleMedium)
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = stringResource(R.string.round_results_title, results.roundNumber),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.round_results_body, results.nextRound),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(results.standings, key = { _, standing -> standing.playerId }) { index, standing ->
                ResultCard(standing = standing, delayMs = index * Motion.STAGGER_MS, animate = !reducedMotion)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(standing: StandingUi, delayMs: Int, animate: Boolean, modifier: Modifier = Modifier) {
    val appeared = remember { MutableTransitionState(!animate) }
    val score = remember { Animatable(if (animate) 0f else standing.score.toFloat()) }
    var titlesShown by remember { mutableStateOf(!animate) }

    LaunchedEffect(Unit) {
        if (!animate) return@LaunchedEffect
        delay(delayMs.toLong())
        appeared.targetState = true
        score.animateTo(standing.score.toFloat(), tween(Motion.COUNT_UP_MS))
        titlesShown = true
    }

    AnimatedVisibility(
        visibleState = appeared,
        enter = fadeIn(tween(Motion.QUICK_MS)) + slideInVertically(tween(Motion.QUICK_MS)) { it / 2 },
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (standing.isKing && titlesShown) {
                    DominoThemeExtras.gameColors.kingContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ),
        ) {
            Row(
                modifier = Modifier.padding(Spacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                Text(
                    text = stringResource(R.string.rank_number, standing.rank),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 32.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = standing.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AnimatedVisibility(
                        visible = titlesShown && (standing.isKing || standing.isKooz),
                        enter = fadeIn() + expandVertically(),
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            if (standing.isKing) KingBadge()
                            if (standing.isKooz) KoozBadge()
                        }
                    }
                    AnimatedVisibility(visible = titlesShown, enter = fadeIn() + expandVertically()) {
                        PlayerMessageLine(standing.message)
                    }
                }
                Text(
                    text = score.value.roundToInt().toString(),
                    style = MaterialTheme.typography.headlineSmall.forNumbers(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun RoundResultsPreview() {
    DominoTheme {
        RoundResultsContent(
            results = RoundResultsUiState.Ready(
                roundNumber = 2,
                nextRound = 3,
                standings = listOf(
                    StandingUi(
                        1,
                        rank = 1,
                        name = "Ahmed",
                        score = 95,
                        colorIndex = 0,
                        qualified = false,
                        isKing = true,
                    ),
                    StandingUi(2, rank = 2, name = "Khaled", score = 40, colorIndex = 1, qualified = false),
                    StandingUi(
                        3,
                        rank = 3,
                        name = "Ali",
                        score = -10,
                        colorIndex = 2,
                        qualified = false,
                        isKooz = true,
                    ),
                ),
            ),
            onContinue = {},
        )
    }
}
