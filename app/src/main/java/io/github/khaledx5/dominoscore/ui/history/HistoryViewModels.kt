package io.github.khaledx5.dominoscore.ui.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.domain.history.GetGameDetailUseCase
import io.github.khaledx5.dominoscore.domain.history.ObserveGameHistoryUseCase
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.rules.GameSnapshot
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.components.toStandings
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One past game in the list. [winnerName] is null for a game that was abandoned. */
@Immutable
data class PastGameUi(
    val gameId: Long,
    val startedAt: Instant,
    val target: Int,
    val winnerName: String?,
    val standings: List<StandingUi>,
)

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data class Ready(val games: List<PastGameUi>) : HistoryUiState
}

internal fun GameSnapshot.toPastGameUi() = PastGameUi(
    gameId = game.id.value,
    startedAt = game.startedAt,
    target = game.target.value,
    winnerName = game.winnerId
        ?.takeIf { game.status == GameStatus.COMPLETED }
        ?.let { id -> game.player(id)?.name?.value },
    standings = toStandings(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(observeHistory: ObserveGameHistoryUseCase) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = observeHistory()
        .map { games -> HistoryUiState.Ready(games.map { it.toPastGameUi() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** One round in the detail: each player's points in it and their total after it, in seat order. */
@Immutable
data class RoundRowUi(val number: Int, val completed: Boolean, val cells: List<RoundCellUi>)

@Immutable
data class RoundCellUi(val name: String, val points: Int, val total: Int)

sealed interface GameDetailUiState {
    data object Loading : GameDetailUiState

    data object Missing : GameDetailUiState

    @Immutable
    data class Ready(val game: PastGameUi, val rounds: List<RoundRowUi>) : GameDetailUiState
}

@HiltViewModel
class GameDetailViewModel @Inject constructor(savedStateHandle: SavedStateHandle, getDetail: GetGameDetailUseCase) :
    ViewModel() {

    private val gameId = GameId(checkNotNull(savedStateHandle.get<Long>(GAME_ID_ARG)) { "The route has a game id" })
    private val _uiState = MutableStateFlow<GameDetailUiState>(GameDetailUiState.Loading)
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val detail = getDetail(gameId)
            _uiState.value = if (detail == null) {
                GameDetailUiState.Missing
            } else {
                val players = detail.snapshot.game.players
                GameDetailUiState.Ready(
                    game = detail.snapshot.toPastGameUi(),
                    rounds = detail.rounds.map { round ->
                        RoundRowUi(
                            number = round.number,
                            completed = round.completed,
                            cells = players.map { player ->
                                RoundCellUi(
                                    name = player.name.value,
                                    points = round.pointsByPlayer.getValue(player.id),
                                    total = round.totalsAfter.getValue(player.id),
                                )
                            },
                        )
                    },
                )
            }
        }
    }

    internal companion object {
        /** Property name in GameDetailDestination: type-safe routes store each argument under it. */
        const val GAME_ID_ARG = "gameId"
    }
}
