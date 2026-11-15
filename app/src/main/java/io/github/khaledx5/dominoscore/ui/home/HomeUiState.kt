package io.github.khaledx5.dominoscore.ui.home

import androidx.compose.runtime.Immutable
import io.github.khaledx5.dominoscore.domain.rules.GameSnapshot
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.components.toStandings

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(val activeGame: ActiveGameUi?) : HomeUiState
}

/** The game left in progress, shown on the home screen so the table can pick it up again. */
@Immutable
data class ActiveGameUi(val roundNumber: Int, val target: Int, val standings: List<StandingUi>)

internal fun GameSnapshot.toActiveGameUi(): ActiveGameUi = ActiveGameUi(
    roundNumber = checkNotNull(game.currentRound) { "An active game always has a round in progress" }.number,
    target = game.target.value,
    standings = toStandings(),
)
