package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.ScoreAmount
import io.github.khaledx5.dominoscore.domain.model.ScoreOperation
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.rules.applyTo
import java.time.Clock
import javax.inject.Inject

class RecordScoreUseCase @Inject constructor(private val repository: GameRepository, private val clock: Clock) {
    /** Adds or subtracts [amount] for one player in the round in progress and returns the saved operation. */
    suspend operator fun invoke(
        playerId: PlayerId,
        type: ScoreOperationType,
        amount: Int,
    ): Outcome<ScoreOperation, RecordScoreError> {
        val scoreAmount = when (val outcome = ScoreAmount.of(amount)) {
            is Outcome.Success -> outcome.value
            is Outcome.Failure -> return Outcome.Failure(InvalidAmount(outcome.error))
        }
        val game = repository.findActiveGame() ?: return Outcome.Failure(NoActiveGame)
        val player = game.player(playerId) ?: return Outcome.Failure(UnknownPlayer)
        val newScore = when (val outcome = type.applyTo(player.score, scoreAmount)) {
            is Outcome.Success -> outcome.value
            is Outcome.Failure -> return Outcome.Failure(ScoreLimitReached)
        }
        val operation = ScoreOperation(
            roundId = game.roundInProgress().id,
            playerId = player.id,
            type = type,
            amount = scoreAmount,
            previousScore = player.score,
            newScore = newScore,
            createdAt = clock.instant(),
        )
        repository.recordScore(game.id, operation)
        return Outcome.Success(operation)
    }
}
