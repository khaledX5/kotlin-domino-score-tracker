package io.github.khaledx5.dominoscore.ui.components

import androidx.compose.runtime.Immutable
import io.github.khaledx5.dominoscore.domain.messages.RankingMessage
import io.github.khaledx5.dominoscore.domain.model.PlayerId
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
    /** The player's latest ranking message, when it fits their current title. */
    val message: PlayerMessageUi? = null,
)

/** The standings in ranking order, best first, each with the player's latest line from [messages]. */
fun GameSnapshot.toStandings(messages: Map<PlayerId, RankingMessage> = emptyMap()): List<StandingUi> =
    ranking.map { (rank, player, isKing, isKooz) ->
        StandingUi(
            playerId = player.id.value,
            rank = rank,
            name = player.name.value,
            score = player.score,
            colorIndex = player.colorIndex,
            qualified = player.isQualified(game.target),
            isKing = isKing,
            isKooz = isKooz,
            message = messages[player.id]?.toUiFor(isKing, isKooz),
        )
    }
