package io.github.khaledx5.dominoscore.ui.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.domain.history.CompletedGame
import io.github.khaledx5.dominoscore.domain.history.GetGameDetailUseCase
import io.github.khaledx5.dominoscore.domain.history.ObservePlayerCountHistoryUseCase
import io.github.khaledx5.dominoscore.domain.history.PlayerCountHistory
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.rules.GameSnapshot
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.components.toStandings
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The game shown on the detail screen. [winnerName] is null for a game that was abandoned. */
@Immutable
data class PastGameUi(
    val gameId: Long,
    val startedAt: Instant,
    val target: Int,
    val winnerName: String?,
    val standings: List<StandingUi>,
)

internal fun GameSnapshot.toPastGameUi() = PastGameUi(
    gameId = game.id.value,
    startedAt = game.startedAt,
    target = game.target.value,
    winnerName = game.winnerId
        ?.takeIf { game.status == GameStatus.COMPLETED }
        ?.let { id -> game.player(id)?.name?.value },
    standings = toStandings(),
)

/** One completed game in the history list, with its standings as they were frozen. */
@Immutable
data class CompletedGameUi(
    val gameId: Long,
    val completedAt: Instant,
    val durationMinutes: Long,
    val rounds: Int,
    val winnerName: String,
    val winnerScore: Int,
    val standings: List<StandingUi>,
)

internal fun CompletedGame.toUi() = CompletedGameUi(
    gameId = id.value,
    completedAt = completedAt,
    durationMinutes = duration.toMinutes(),
    rounds = roundCount,
    winnerName = winner.name.value,
    winnerScore = winner.score,
    standings = standings.map {
        StandingUi(
            playerId = it.playerId.value,
            rank = it.rank,
            name = it.name.value,
            score = it.score,
            colorIndex = it.colorIndex,
            qualified = false,
            isKing = it.isKing,
            isKooz = it.isKooz,
        )
    },
)

sealed interface HistoryContentState {
    /** Before the first read of the selected category. */
    data object Loading : HistoryContentState

    @Immutable
    data class Ready(val games: List<CompletedGameUi>, val statistics: StatisticsUi) : HistoryContentState
}

enum class HistoryTab {
    HISTORY,
    STATISTICS,
}

/**
 * The selected table size, the selected tab and what was played at that size. Every size is its
 * own category; the tab stays when the size changes.
 */
@Immutable
data class HistoryUiState(
    val playerCount: Int = DEFAULT_PLAYER_COUNT,
    val tab: HistoryTab = HistoryTab.HISTORY,
    val content: HistoryContentState = HistoryContentState.Loading,
) {
    companion object {
        val DEFAULT_PLAYER_COUNT = Game.PLAYER_COUNT.first
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeHistory: ObservePlayerCountHistoryUseCase,
) : ViewModel() {

    private val playerCount = savedStateHandle.getStateFlow(PLAYER_COUNT_KEY, HistoryUiState.DEFAULT_PLAYER_COUNT)

    private val tab = savedStateHandle.getStateFlow(TAB_KEY, HistoryTab.HISTORY)

    val uiState: StateFlow<HistoryUiState> = playerCount
        .flatMapLatest { count ->
            observeHistory(count)
                .map<PlayerCountHistory, HistoryContentState> { history ->
                    HistoryContentState.Ready(history.games.map { it.toUi() }, history.toStatisticsUi())
                }
                .onStart { emit(HistoryContentState.Loading) }
                .map { content -> count to content }
        }
        .combine(tab) { (count, content), selectedTab ->
            HistoryUiState(playerCount = count, tab = selectedTab, content = content)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState())

    fun selectPlayerCount(count: Int) {
        require(count in Game.PLAYER_COUNT) { "Unsupported player count $count" }
        savedStateHandle[PLAYER_COUNT_KEY] = count
    }

    fun selectTab(tab: HistoryTab) {
        savedStateHandle[TAB_KEY] = tab
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val PLAYER_COUNT_KEY = "playerCount"
        const val TAB_KEY = "tab"
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
