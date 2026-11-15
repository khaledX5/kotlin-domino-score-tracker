package io.github.khaledx5.dominoscore.domain.history

import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.Profile
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import java.time.Duration
import java.time.Instant

/**
 * One player's result as it was frozen when the winner was confirmed. [name] is the name shown in
 * that game (after any rename); [profile] is the person behind it across games.
 */
data class FinalStanding(
    val playerId: PlayerId,
    val profile: Profile,
    val name: PlayerName,
    val score: Int,
    val rank: Int,
    val isKing: Boolean,
    val isKooz: Boolean,
)

/** A game the table finished with a confirmed winner. [standings] are best first. */
data class CompletedGame(
    val id: GameId,
    val target: TargetScore,
    val startedAt: Instant,
    val completedAt: Instant,
    val roundCount: Int,
    val winnerId: PlayerId,
    val standings: List<FinalStanding>,
) {
    init {
        require(standings.size in Game.PLAYER_COUNT) { "A game needs ${Game.PLAYER_COUNT} players" }
        val winnerIsSeated = standings.any { it.playerId == winnerId }
        require(winnerIsSeated) { "The winner of game ${id.value} is not one of its players" }
    }

    val playerCount: Int get() = standings.size

    val winner: FinalStanding get() = standings.first { it.playerId == winnerId }

    val duration: Duration get() = Duration.between(startedAt, completedAt)
}
