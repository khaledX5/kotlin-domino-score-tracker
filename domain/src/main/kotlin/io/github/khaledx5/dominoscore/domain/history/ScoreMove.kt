package io.github.khaledx5.dominoscore.domain.history

import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** One Add or Penalty as the table saw it: who, in which round, how much, and the total it left. */
data class ScoreMove(
    val id: Long,
    val roundNumber: Int,
    val playerId: PlayerId,
    val type: ScoreOperationType,
    val amount: Int,
    val newScore: Int,
    val createdAt: Instant,
) {
    /** Signed: positive for Add, negative for Penalty. */
    val points: Int get() = if (type == ScoreOperationType.ADD) amount else -amount
}

class ObserveMovesUseCase @Inject constructor(private val repository: GameRepository) {
    /** Every move of [gameId], newest first, emitted again after each new one. */
    operator fun invoke(gameId: GameId): Flow<List<ScoreMove>> = repository.observeMoves(gameId)
}
