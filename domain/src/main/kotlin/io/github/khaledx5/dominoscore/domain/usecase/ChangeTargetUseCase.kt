package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import java.time.Clock
import javax.inject.Inject

/** Moves the target mid-game. Qualification is derived from it, so it updates on the next emission. */
class ChangeTargetUseCase @Inject constructor(private val repository: GameRepository, private val clock: Clock) {
    suspend operator fun invoke(newTarget: Int): Outcome<TargetScore, ChangeTargetError> {
        val target = when (val outcome = TargetScore.of(newTarget)) {
            is Outcome.Success -> outcome.value
            is Outcome.Failure -> return Outcome.Failure(InvalidTarget(outcome.error))
        }
        val game = repository.findActiveGame() ?: return Outcome.Failure(NoActiveGame)
        if (game.target == target) return Outcome.Failure(TargetUnchanged)
        repository.changeTarget(game.id, target, clock.instant())
        return Outcome.Success(target)
    }
}
