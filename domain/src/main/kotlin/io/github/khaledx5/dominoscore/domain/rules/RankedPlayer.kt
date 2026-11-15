package io.github.khaledx5.dominoscore.domain.rules

import io.github.khaledx5.dominoscore.domain.model.Player

/**
 * A player's place in the standings. [isKing] marks everyone on the highest score and [isKooz]
 * everyone on the lowest; when all scores are equal, every player is both.
 */
data class RankedPlayer(val rank: Int, val player: Player, val isKing: Boolean = false, val isKooz: Boolean = false)

private val bestFirst = compareByDescending<Player> { it.score }.thenBy { it.seat }

/**
 * Standard competition ranking of cumulative scores: equal scores share a rank and the next
 * rank skips (150, 100, 100, 50 -> 1, 2, 2, 4). Seat order only breaks ties for display.
 * KING and KOOZ come from the scores themselves, never from seat or id.
 */
fun rankPlayers(players: List<Player>): List<RankedPlayer> {
    val ordered = players.sortedWith(bestFirst)
    val highest = ordered.firstOrNull()?.score
    val lowest = ordered.lastOrNull()?.score
    return ordered.map { player ->
        val playersAhead = ordered.count { it.score > player.score }
        RankedPlayer(
            rank = playersAhead + 1,
            player = player,
            isKing = player.score == highest,
            isKooz = player.score == lowest,
        )
    }
}
