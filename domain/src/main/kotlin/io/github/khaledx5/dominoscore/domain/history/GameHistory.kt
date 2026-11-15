package io.github.khaledx5.dominoscore.domain.history

import io.github.khaledx5.dominoscore.domain.messages.ScoreLog
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.ScoreOperation
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.rules.GameSnapshot
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One round of a past game: what each player won or lost in it, and the totals after it. */
data class RoundBreakdown(
    val number: Int,
    val pointsByPlayer: Map<PlayerId, Int>,
    val totalsAfter: Map<PlayerId, Int>,
    /** False for the round an abandoned game stopped in. */
    val completed: Boolean = true,
)

/**
 * Splits the score log into rounds. Every game starts on 0, so each round's points are the
 * changes logged in it and the totals are a running sum. Rounds with no score show zeros.
 */
fun breakDownRounds(players: List<Player>, log: ScoreLog): List<RoundBreakdown> {
    val totals = players.associate { it.id to 0 }.toMutableMap()
    val operationsByRound = log.operations.groupBy { it.roundId }

    fun round(number: Int, operations: List<ScoreOperation>, done: Boolean): RoundBreakdown {
        val points = players.associate { it.id to 0 }.toMutableMap()
        operations.filter { it.playerId in totals }.forEach { operation ->
            points[operation.playerId] =
                points.getValue(operation.playerId) + (operation.newScore - operation.previousScore)
            totals[operation.playerId] = operation.newScore
        }
        return RoundBreakdown(number, points.toMap(), totals.toMap(), done)
    }

    val completed = log.completedRounds.sortedBy { it.number }
    val rounds = completed.map { round(it.number, operationsByRound[it.id].orEmpty(), done = true) }
    val completedIds = completed.map { it.id }.toSet()
    val unfinished = log.operations.filter { it.roundId !in completedIds }
    return if (unfinished.isEmpty()) rounds else rounds + round(completed.size + 1, unfinished, done = false)
}

/** A finished or abandoned game with its ranking and its rounds. */
data class GameDetail(val snapshot: GameSnapshot, val rounds: List<RoundBreakdown>)

class ObserveGameHistoryUseCase @Inject constructor(private val repository: GameRepository) {
    /** Every game that is no longer in progress, newest first. */
    operator fun invoke(): Flow<List<GameSnapshot>> =
        repository.observeFinishedGames().map { games -> games.map(GameSnapshot::of) }
}

class GetGameDetailUseCase @Inject constructor(private val repository: GameRepository) {
    suspend operator fun invoke(gameId: GameId): GameDetail? {
        val game = repository.findGame(gameId) ?: return null
        return GameDetail(GameSnapshot.of(game), breakDownRounds(game.players, repository.findScoreLog(gameId)))
    }
}
