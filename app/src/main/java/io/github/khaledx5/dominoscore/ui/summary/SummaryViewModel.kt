package io.github.khaledx5.dominoscore.ui.summary

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.usecase.GetGameUseCase
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.components.toStandings
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SummaryUiState {
    data object Loading : SummaryUiState

    /** The game is missing or has no winner: nothing to celebrate, the screen closes. */
    data object Unavailable : SummaryUiState

    @Immutable
    data class Ready(val winner: StandingUi, val standings: List<StandingUi>) : SummaryUiState
}

/** Reads the finished game by the id in its route, so the screen comes back the same after a process death. */
@HiltViewModel
class SummaryViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val getGame: GetGameUseCase) :
    ViewModel() {

    private val gameId = GameId(checkNotNull(savedStateHandle.get<Long>(GAME_ID_ARG)) { "The route has a game id" })

    private val _uiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { _uiState.value = load() }
    }

    private suspend fun load(): SummaryUiState {
        val snapshot = getGame(gameId) ?: return SummaryUiState.Unavailable
        val winnerId = snapshot.game.winnerId ?: return SummaryUiState.Unavailable
        val standings = snapshot.toStandings()
        val winner = standings.firstOrNull { it.playerId == winnerId.value } ?: return SummaryUiState.Unavailable
        return SummaryUiState.Ready(winner = winner, standings = standings)
    }

    internal companion object {
        /** Property name in SummaryDestination: type-safe routes store each argument under it. */
        const val GAME_ID_ARG = "gameId"
    }
}
