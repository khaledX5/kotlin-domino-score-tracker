package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.rules.WinnerSelection
import io.github.khaledx5.dominoscore.domain.rules.rankPlayers
import io.github.khaledx5.dominoscore.domain.rules.selectWinner
import java.time.Clock
import javax.inject.Inject

class ConfirmWinnerUseCase @Inject constructor(private val repository: GameRepository, private val clock: Clock) {
    /**
     * Ends the game with [winnerId], who must be one of the current winner candidates. The final
     * ranking, KING and KOOZ are saved with it.
     */
    suspend operator fun invoke(winnerId: PlayerId): Outcome<Unit, ConfirmWinnerError> {
        val game = repository.findActiveGame() ?: return Outcome.Failure(NoActiveGame)
        val selection = selectWinner(game.players, game.target)
        return when {
            selection == WinnerSelection.NobodyQualified -> Outcome.Failure(NobodyQualified)

            !selection.allows(winnerId) -> Outcome.Failure(NotACandidate)

            else -> {
                repository.completeGame(game.id, winnerId, rankPlayers(game.players), clock.instant())
                Outcome.Success(Unit)
            }
        }
    }
}
