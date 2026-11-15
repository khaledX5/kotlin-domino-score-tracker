package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import java.time.Clock
import javax.inject.Inject

class RenamePlayerUseCase @Inject constructor(private val repository: GameRepository, private val clock: Clock) {
    /** Renames a player of the active game. Saving the same name again is a no-op. */
    suspend operator fun invoke(playerId: PlayerId, newName: String): Outcome<PlayerName, RenamePlayerError> {
        val name = when (val outcome = PlayerName.of(newName)) {
            is Outcome.Success -> outcome.value
            is Outcome.Failure -> return Outcome.Failure(InvalidName(outcome.error))
        }
        val game = repository.findActiveGame() ?: return Outcome.Failure(NoActiveGame)
        val player = game.player(playerId) ?: return Outcome.Failure(UnknownPlayer)
        if (game.players.any { it.id != playerId && it.name.matches(name) }) return Outcome.Failure(NameTaken)
        if (player.name != name) repository.renamePlayer(game.id, playerId, name, clock.instant())
        return Outcome.Success(name)
    }
}
