package io.github.khaledx5.dominoscore.domain.rules

import io.github.khaledx5.dominoscore.domain.model.Player

data class RankedPlayer(val rank: Int, val player: Player)

private val bestFirst = compareByDescending<Player> { it.score }.thenBy { it.seat }

/**
 * Standard competition ranking of cumulative scores: equal scores share a rank and the next
 * rank skips (150, 100, 100, 50 -> 1, 2, 2, 4). Seat order only breaks ties for display.
 */
fun rankPlayers(players: List<Player>): List<RankedPlayer> {
    val ordered = players.sortedWith(bestFirst)
    return ordered.map { player ->
        val playersAhead = ordered.count { it.score > player.score }
        RankedPlayer(rank = playersAhead + 1, player = player)
    }
}
