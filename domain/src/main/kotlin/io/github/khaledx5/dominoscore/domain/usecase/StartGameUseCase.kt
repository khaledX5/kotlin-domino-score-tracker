package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerColors
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.repository.NewGame
import io.github.khaledx5.dominoscore.domain.rules.validateRoster
import java.time.Clock
import javax.inject.Inject

class StartGameUseCase @Inject constructor(private val repository: GameRepository, private val clock: Clock) {
    /**
     * Starts a game with the names in seat order. While another game is active this fails with
     * [ActiveGameExists] unless [replaceActiveGame] is true, which marks the old game ABANDONED.
     * [colorIndexes] are the colors the table picked, one per seat; null means seat order.
     */
    suspend operator fun invoke(
        targetScore: Int,
        playerNames: List<String>,
        replaceActiveGame: Boolean = false,
        colorIndexes: List<Int>? = null,
    ): Outcome<GameId, StartGameError> {
        val target = when (val outcome = TargetScore.of(targetScore)) {
            is Outcome.Success -> outcome.value
            is Outcome.Failure -> return Outcome.Failure(InvalidTarget(outcome.error))
        }
        val names = when (val outcome = validateRoster(playerNames)) {
            is Outcome.Success -> outcome.value
            is Outcome.Failure -> return Outcome.Failure(InvalidRoster(outcome.error))
        }
        val colors = colorIndexes ?: PlayerColors.defaultFor(names.size)
        if (colors.size != names.size || !PlayerColors.areValid(colors)) {
            return Outcome.Failure(InvalidColors)
        }
        if (!replaceActiveGame && repository.findActiveGame() != null) {
            return Outcome.Failure(ActiveGameExists)
        }
        val gameId = repository.startGame(
            NewGame(target = target, playerNames = names, startedAt = clock.instant(), colorIndexes = colors),
        )
        return Outcome.Success(gameId)
    }
}
