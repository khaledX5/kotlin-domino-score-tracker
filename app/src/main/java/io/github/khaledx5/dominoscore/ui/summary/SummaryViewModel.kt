package io.github.khaledx5.dominoscore.ui.summary

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.usecase.GetGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObservePlayerMessagesUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.share.ResultCard
import io.github.khaledx5.dominoscore.share.ResultImageExporter
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.components.toStandings
import java.io.File
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SummaryUiState {
    data object Loading : SummaryUiState

    /** The game is missing or has no winner: nothing to celebrate, the screen closes. */
    data object Unavailable : SummaryUiState

    /**
     * [standings] are the final ranking with KING / KOOZ and each player's last line. [replayNames]
     * and [replayTarget] start the same table again; [replayStarted] tells the screen to open it.
     * [pendingShare] is a rendered result image waiting for the share sheet.
     */
    @Immutable
    data class Ready(
        val winner: StandingUi,
        val standings: List<StandingUi>,
        val replayNames: List<String> = emptyList(),
        val replayTarget: Int = 0,
        val replayColors: List<Int> = emptyList(),
        val replayStarted: Boolean = false,
        val completedAt: Instant? = null,
        val isSharing: Boolean = false,
        val pendingShare: File? = null,
        val shareFailed: Boolean = false,
    ) : SummaryUiState
}

/** Reads the finished game by the id in its route, so the screen comes back the same after a process death. */
@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGame: GetGameUseCase,
    private val observeMessages: ObservePlayerMessagesUseCase,
    private val startGame: StartGameUseCase,
    private val exporter: ResultImageExporter,
) : ViewModel() {

    private val gameId = GameId(checkNotNull(savedStateHandle.get<Long>(GAME_ID_ARG)) { "The route has a game id" })

    private val _uiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { _uiState.value = load() }
    }

    private suspend fun load(): SummaryUiState {
        val snapshot = getGame(gameId) ?: return SummaryUiState.Unavailable
        val winnerId = snapshot.game.winnerId ?: return SummaryUiState.Unavailable
        val standings = snapshot.toStandings(observeMessages(gameId).first())
        val winner = standings.firstOrNull { it.playerId == winnerId.value } ?: return SummaryUiState.Unavailable
        return SummaryUiState.Ready(
            winner = winner,
            standings = standings,
            replayNames = snapshot.game.players.map { it.name.value },
            replayTarget = snapshot.game.target.value,
            replayColors = snapshot.game.players.map { it.colorIndex },
            completedAt = snapshot.game.completedAt,
        )
    }

    /** Renders [card] off the main thread; the screen opens the share sheet once the file is ready. */
    fun share(card: ResultCard) {
        val ready = _uiState.value as? SummaryUiState.Ready ?: return
        if (ready.isSharing) return
        updateReady { it.copy(isSharing = true, shareFailed = false) }
        viewModelScope.launch {
            val file = try {
                exporter.export(card)
            } catch (_: IOException) {
                // A full disk is the realistic cause; the screen says the image could not be made.
                null
            }
            updateReady { it.copy(isSharing = false, pendingShare = file, shareFailed = file == null) }
        }
    }

    fun onShareHandled() = updateReady { it.copy(pendingShare = null, shareFailed = false) }

    private fun updateReady(change: (SummaryUiState.Ready) -> SummaryUiState.Ready) =
        _uiState.update { (it as? SummaryUiState.Ready)?.let(change) ?: it }

    /** Same names and colors in the same seats, same target, scores back to zero. */
    fun playAgain() {
        val ready = _uiState.value as? SummaryUiState.Ready ?: return
        if (ready.replayStarted) return
        viewModelScope.launch {
            // The finished game is no longer active, so nothing is replaced here.
            if (startGame(
                    ready.replayTarget,
                    ready.replayNames,
                    colorIndexes = ready.replayColors,
                ) is Outcome.Success
            ) {
                _uiState.update { (it as? SummaryUiState.Ready)?.copy(replayStarted = true) ?: it }
            }
        }
    }

    internal companion object {
        /** Property name in SummaryDestination: type-safe routes store each argument under it. */
        const val GAME_ID_ARG = "gameId"
    }
}
