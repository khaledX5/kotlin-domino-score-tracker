package io.github.khaledx5.dominoscore.domain.messages

import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.Round
import io.github.khaledx5.dominoscore.domain.model.ScoreOperation
import io.github.khaledx5.dominoscore.domain.rules.rankPlayers

/** The completed rounds and the score log of one game, oldest first. */
data class ScoreLog(val completedRounds: List<Round> = emptyList(), val operations: List<ScoreOperation> = emptyList())

/** What a player went through earlier in the game, as far as KING and KOOZ go. */
data class TitleHistory(
    val everKing: Boolean = false,
    val everKooz: Boolean = false,
    /** Completed rounds in a row, up to the latest one, that ended with the player as KING. */
    val kingStreakRounds: Int = 0,
    /** The player was KOOZ when some earlier round ended. */
    val koozAtARoundEnd: Boolean = false,
)

/**
 * Replays the score log from the all-zero start, ranking every step with [rankPlayers], so titles
 * are never worked out a second way. Every game starts on 0 and the log keeps each new score.
 */
class TitleTimeline(private val players: List<Player>, log: ScoreLog) {

    private val scores = players.associate { it.id to 0 }.toMutableMap()
    private val history = players.associate { it.id to TitleHistory() }.toMutableMap()

    /** Each player's history over the whole log. */
    val untilNow: Map<PlayerId, TitleHistory>

    /** Each player's history when the latest completed round ended (the start of the game if none did). */
    val untilLastRoundEnd: Map<PlayerId, TitleHistory>

    /** Everyone's score when the latest completed round ended: what a round end is compared with. */
    val lastRoundEnd: List<Player>

    init {
        val operationsByRound = log.operations.groupBy { it.roundId }
        var roundEndHistory = history.toMap()
        var roundEndPlayers = currentPlayers()
        for (round in log.completedRounds.sortedBy { it.number }) {
            operationsByRound[round.id].orEmpty().forEach(::apply)
            markRoundEnd()
            roundEndHistory = history.toMap()
            roundEndPlayers = currentPlayers()
        }
        val completedIds = log.completedRounds.map { it.id }.toSet()
        log.operations.filter { it.roundId !in completedIds }.forEach(::apply)

        untilNow = history.toMap()
        untilLastRoundEnd = roundEndHistory
        lastRoundEnd = roundEndPlayers
    }

    private fun currentPlayers(): List<Player> = players.map { it.copy(score = scores.getValue(it.id)) }

    private fun apply(operation: ScoreOperation) {
        if (operation.playerId !in scores) return
        scores[operation.playerId] = operation.newScore
        rankPlayers(currentPlayers()).forEach { ranked ->
            history.computeIfPresent(ranked.player.id) { _, seen ->
                seen.copy(everKing = seen.everKing || ranked.isKing, everKooz = seen.everKooz || ranked.isKooz)
            }
        }
    }

    private fun markRoundEnd() {
        rankPlayers(currentPlayers()).forEach { ranked ->
            history.computeIfPresent(ranked.player.id) { _, seen ->
                seen.copy(
                    kingStreakRounds = if (ranked.isKing) seen.kingStreakRounds + 1 else 0,
                    koozAtARoundEnd = seen.koozAtARoundEnd || ranked.isKooz,
                )
            }
        }
    }
}
