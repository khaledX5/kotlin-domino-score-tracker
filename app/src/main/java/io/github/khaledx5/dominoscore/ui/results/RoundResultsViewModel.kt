package io.github.khaledx5.dominoscore.ui.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.ui.components.toStandings
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Reads the round number from its route and the standings from the active game, so the screen
 * is rebuilt from the database after a process death instead of from data handed over in memory.
 */
@HiltViewModel
class RoundResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeActiveGame: ObserveActiveGameUseCase,
) : ViewModel() {

    private val roundNumber: Int = checkNotNull(savedStateHandle[ROUND_NUMBER_ARG]) { "The route has a round number" }

    val uiState: StateFlow<RoundResultsUiState> = observeActiveGame()
        .map { snapshot ->
            val nextRound = snapshot?.game?.currentRound?.number
            if (snapshot == null || nextRound == null) {
                RoundResultsUiState.NoActiveGame
            } else {
                RoundResultsUiState.Ready(
                    roundNumber = roundNumber,
                    nextRound = nextRound,
                    standings = snapshot.toStandings(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RoundResultsUiState.Loading)

    internal companion object {
        /** Property name in RoundResultsDestination: type-safe routes store each argument under it. */
        const val ROUND_NUMBER_ARG = "roundNumber"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
