package io.github.khaledx5.dominoscore.ui.home

import androidx.compose.runtime.Immutable
import io.github.khaledx5.dominoscore.domain.rules.GameSnapshot
import io.github.khaledx5.dominoscore.domain.rules.isQualified

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(val activeGame: ActiveGameUi?) : HomeUiState
}

/** The game left in progress, shown on the home screen so the table can pick it up again. */
@Immutable
data class ActiveGameUi(val roundNumber: Int, val target: Int, val standings: List<StandingUi>)

@Immutable
data class StandingUi(
    val playerId: Long,
    val rank: Int,
    val name: String,
    val score: Int,
    val colorIndex: Int,
    val qualified: Boolean,
    val isKing: Boolean = false,
    val isKooz: Boolean = false,
)

internal fun GameSnapshot.toActiveGameUi(): ActiveGameUi = ActiveGameUi(
    roundNumber = checkNotNull(game.currentRound) { "An active game always has a round in progress" }.number,
    target = game.target.value,
    standings = ranking.map { (rank, player, isKing, isKooz) ->
        StandingUi(
            playerId = player.id.value,
            rank = rank,
            name = player.name.value,
            score = player.score,
            colorIndex = player.colorIndex,
            qualified = player.isQualified(game.target),
            isKing = isKing,
            isKooz = isKooz,
        )
    },
)
