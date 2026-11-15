package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.rules.PrimaryAction
import io.github.khaledx5.dominoscore.domain.rules.primaryActionFor
import java.time.Clock
import javax.inject.Inject

class EndRoundUseCase @Inject constructor(private val repository: GameRepository, private val clock: Clock) {
    /** Completes the round in progress and returns its number. Not allowed once anyone is qualified. */
    suspend operator fun invoke(): Outcome<Int, EndRoundError> {
        val game = repository.findActiveGame() ?: return Outcome.Failure(NoActiveGame)
        if (primaryActionFor(game.players, game.target) == PrimaryAction.END_GAME) {
            return Outcome.Failure(PlayerQualified)
        }
        val round = game.roundInProgress()
        repository.completeRound(game.id, round.id, clock.instant())
        return Outcome.Success(round.number)
    }
}
