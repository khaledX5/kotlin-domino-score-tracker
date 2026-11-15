package io.github.khaledx5.dominoscore.ui.components

import androidx.compose.runtime.Immutable
import io.github.khaledx5.dominoscore.domain.rules.GameSnapshot
import io.github.khaledx5.dominoscore.domain.rules.isQualified

/** One player as the standings show them: place, KING / KOOZ titles and whether they reached the target. */
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

/** The standings in ranking order, best first. */
fun GameSnapshot.toStandings(): List<StandingUi> = ranking.map { (rank, player, isKing, isKooz) ->
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
}
