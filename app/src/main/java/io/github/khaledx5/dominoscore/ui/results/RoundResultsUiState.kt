package io.github.khaledx5.dominoscore.ui.results

import androidx.compose.runtime.Immutable
import io.github.khaledx5.dominoscore.ui.components.StandingUi

sealed interface RoundResultsUiState {
    data object Loading : RoundResultsUiState

    /** The game was finished or replaced meanwhile: the screen closes itself. */
    data object NoActiveGame : RoundResultsUiState

    /** [standings] are cumulative scores after [roundNumber], best first; [nextRound] is already in progress. */
    @Immutable
    data class Ready(val roundNumber: Int, val nextRound: Int, val standings: List<StandingUi>) : RoundResultsUiState
}
