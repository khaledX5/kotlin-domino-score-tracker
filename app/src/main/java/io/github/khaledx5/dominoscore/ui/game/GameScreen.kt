package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.ui.components.StandingRow
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.Spacing

@Composable
fun GameScreen(
    onBack: () -> Unit,
    onGameClose: () -> Unit,
    onShowRoundResults: (roundNumber: Int) -> Unit,
    onCelebrate: (gameId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnGameClose by rememberUpdatedState(onGameClose)
    val currentOnShowRoundResults by rememberUpdatedState(onShowRoundResults)
    val currentOnCelebrate by rememberUpdatedState(onCelebrate)

    LaunchedEffect(state) {
        when (val current = state) {
            GameUiState.NoActiveGame -> currentOnGameClose()
            is GameUiState.Finished -> currentOnCelebrate(current.gameId)
            else -> Unit
        }
    }
    val endedRound = (state as? GameUiState.Playing)?.endedRound
    LaunchedEffect(endedRound) {
        if (endedRound != null) {
            currentOnShowRoundResults(endedRound)
            viewModel.onRoundResultsShown()
        }
    }

    when (val current = state) {
        GameUiState.Loading, GameUiState.NoActiveGame, is GameUiState.Finished -> Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is GameUiState.Playing -> {
            GameContent(
                game = current.game,
                music = current.music,
                onBack = onBack,
                onPlayMusic = viewModel::playMusic,
                onAdd = { viewModel.openScoreEntry(it.playerId, it.name, ScoreOperationType.ADD) },
                onPenalty = { viewModel.openScoreEntry(it.playerId, it.name, ScoreOperationType.PENALTY) },
                onEditTarget = { viewModel.openTargetEdit(current.game.target) },
                onPrimaryAction = { viewModel.onPrimaryAction(current.game.primaryAction) },
                onRename = { viewModel.openRename(it.playerId, it.name) },
                modifier = modifier,
            )
            current.rename?.let { edit ->
                RenameDialog(
                    rename = edit,
                    onTextChange = viewModel::updateRenameText,
                    onConfirm = viewModel::confirmRename,
                    onDismiss = viewModel::dismissRename,
                )
            }
            current.winnerPick?.let { pick ->
                WinnerDialog(
                    pick = pick,
                    onSelect = viewModel::selectWinner,
                    onConfirm = viewModel::confirmWinner,
                    onDismiss = viewModel::dismissWinnerPick,
                )
            }
            current.targetEdit?.let { edit ->
                TargetEditDialog(
                    edit = edit,
                    onTextChange = viewModel::updateTargetText,
                    onConfirm = viewModel::confirmTargetChange,
                    onDismiss = viewModel::dismissTargetEdit,
                )
            }
            current.scoreEntry?.let { entry ->
                ScoreEntrySheet(
                    entry = entry,
                    onAmountChange = viewModel::updateAmount,
                    onQuickAmount = viewModel::submitQuickAmount,
                    onSubmit = viewModel::submitAmount,
                    onDismiss = viewModel::dismissScoreEntry,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameContent(
    game: GameUi,
    music: MusicButtonUi,
    onBack: () -> Unit,
    onPlayMusic: () -> Unit,
    onAdd: (StandingUi) -> Unit,
    onPenalty: (StandingUi) -> Unit,
    onEditTarget: () -> Unit,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    onRename: (StandingUi) -> Unit = {},
) {
    KeepScreenOn()
    val haptics = LocalHapticFeedback.current
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.game_round, game.roundNumber)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = {
                    TargetChip(target = game.target, onClick = onEditTarget)
                    if (music.visible) LaughButton(onClick = onPlayMusic)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            PrimaryActionButton(
                action = game.primaryAction,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onPrimaryAction()
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            item(key = "qualified") {
                AnimatedVisibility(
                    visible = game.qualifiedNames.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    QualifiedBanner(names = game.qualifiedNames)
                }
            }
            items(game.players, key = { it.playerId }) { player ->
                PlayerCard(player = player, onRename = { onRename(player) }) {
                    ScoreButtons(onAdd = { onAdd(player) }, onPenalty = { onPenalty(player) })
                }
            }
            item(key = "ranking") {
                LiveRankingCard(standings = game.standings)
            }
        }
    }
}

/** A game is played with the phone on the table: the screen must not go dark between hands. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun TargetChip(target: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.target_dialog_title)
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_flag),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = pluralStringResource(R.plurals.points_value, target, target),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * The 😂 button: every tap plays a laugh once, like a doorbell. It never stays pressed, there is
 * nothing to switch off. The emoji is the icon, so it reads as "laugh" in every language.
 */
@Composable
private fun LaughButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.game_laugh)
    OutlinedIconButton(onClick = onClick, modifier = modifier.semantics { contentDescription = description }) {
        Text(text = stringResource(R.string.game_laugh_emoji), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun LiveRankingCard(standings: List<StandingUi>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                text = stringResource(R.string.game_live_ranking),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.s),
            )
            standings.forEach { StandingRow(it) }
        }
    }
}

@Preview
@Composable
private fun GameContentPreview() {
    val players = listOf(
        StandingUi(
            playerId = 1,
            rank = 1,
            name = "Ahmed",
            score = 95,
            colorIndex = 0,
            qualified = false,
            isKing = true,
        ),
        StandingUi(playerId = 2, rank = 2, name = "Khaled", score = 40, colorIndex = 1, qualified = false),
        StandingUi(playerId = 3, rank = 3, name = "Ali", score = -10, colorIndex = 2, qualified = false, isKooz = true),
    )
    DominoTheme {
        GameContent(
            game = GameUi(roundNumber = 2, target = 150, players = players, standings = players),
            music = MusicButtonUi(visible = true),
            onBack = {},
            onPlayMusic = {},
            onAdd = {},
            onPenalty = {},
            onEditTarget = {},
            onPrimaryAction = {},
        )
    }
}
