package io.github.khaledx5.dominoscore.domain.model

import java.time.Instant

enum class GameStatus {
    ACTIVE,
    COMPLETED,
    ABANDONED,
}

/**
 * One game at a real table. [currentRound] is the round in progress and is null once the game
 * is no longer active.
 */
data class Game(
    val id: GameId,
    val target: TargetScore,
    val status: GameStatus,
    val players: List<Player>,
    val currentRound: Round?,
    val startedAt: Instant,
    val winnerId: PlayerId? = null,
    val completedAt: Instant? = null,
) {
    init {
        require(players.size in PLAYER_COUNT) { "A game needs $PLAYER_COUNT players, got ${players.size}" }
        require(players.distinctBy { it.seat }.size == players.size) { "Two players share a seat" }
    }

    val isActive: Boolean get() = status == GameStatus.ACTIVE

    fun player(id: PlayerId): Player? = players.firstOrNull { it.id == id }

    companion object {
        val PLAYER_COUNT = 2..4
    }
}
