package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.runtime.Immutable
import io.github.khaledx5.dominoscore.domain.rules.GameSnapshot
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.components.toStandings

sealed interface GameUiState {
    data object Loading : GameUiState

    /** The game ended or was replaced: the screen closes itself. */
    data object NoActiveGame : GameUiState

    data class Playing(val game: GameUi, val music: MusicButtonUi) : GameUiState
}

/**
 * [players] stay in seat order, so the Add and Penalty buttons never move under a finger;
 * [standings] are the same players in ranking order for the live ranking card.
 */
@Immutable
data class GameUi(
    val roundNumber: Int,
    val target: Int,
    val players: List<StandingUi>,
    val standings: List<StandingUi>,
)

/** The music button is hidden when gameplay music is switched off in the settings. */
@Immutable
data class MusicButtonUi(val visible: Boolean, val playing: Boolean)

internal fun GameSnapshot.toGameUi(): GameUi {
    val standings = toStandings()
    return GameUi(
        roundNumber = checkNotNull(game.currentRound) { "An active game always has a round in progress" }.number,
        target = game.target.value,
        players = standings.sortedBy { standing -> game.players.indexOfFirst { it.id.value == standing.playerId } },
        standings = standings,
    )
}
