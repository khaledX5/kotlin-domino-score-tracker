package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.domain.history.ObserveMovesUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.ui.theme.Spacing
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The moves of one round, newest first. */
@Immutable
data class MoveRoundUi(val number: Int, val moves: List<MoveUi>)

sealed interface MovesUiState {
    data object Loading : MovesUiState

    /** The game ended or was replaced: the screen closes itself. */
    data object NoActiveGame : MovesUiState

    /** Rounds newest first, so the last move of the game is at the very top. */
    @Immutable
    data class Ready(val rounds: List<MoveRoundUi>) : MovesUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MovesViewModel @Inject constructor(
    observeActiveGame: ObserveActiveGameUseCase,
    observeMoves: ObserveMovesUseCase,
) : ViewModel() {

    val uiState: StateFlow<MovesUiState> = observeActiveGame()
        .flatMapLatest { snapshot ->
            if (snapshot == null) {
                flowOf(MovesUiState.NoActiveGame)
            } else {
                observeMoves(snapshot.game.id).map { moves ->
                    val rows = moves.mapNotNull { it.toUi(snapshot.game.players) }
                    // Moves arrive newest first, so grouping keeps both rounds and moves newest first.
                    MovesUiState.Ready(
                        rows.groupBy {
                            it.roundNumber
                        }.map { (number, inRound) -> MoveRoundUi(number, inRound) },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MovesUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun MovesScreen(
    onBack: () -> Unit,
    onGameClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnGameClose by rememberUpdatedState(onGameClose)
    LaunchedEffect(state) {
        if (state == MovesUiState.NoActiveGame) currentOnGameClose()
    }
    MovesContent(state = state, onBack = onBack, modifier = modifier)
}

/** Every move of the game in progress, round by round, the newest at the top. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovesContent(state: MovesUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.game_history)) },
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
    ) { padding ->
        when (state) {
            MovesUiState.Loading, MovesUiState.NoActiveGame -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is MovesUiState.Ready -> if (state.rounds.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.game_history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.xl),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    items(state.rounds, key = { it.number }) { round -> RoundMovesCard(round) }
                }
            }
        }
    }
}

@Composable
private fun RoundMovesCard(round: MoveRoundUi, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(R.string.game_round, round.number),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.s),
            )
            round.moves.forEach { MoveRow(it, showRound = false) }
        }
    }
}
