package io.github.khaledx5.dominoscore.domain.command

import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Wraps a game command from the outside, so the use cases and their rules stay exactly as they are.
 * It reads the table once before, runs the command, reads it once after, and hands that one
 * [GameTransition] to every listener. Because it runs once per command, flow emissions,
 * recompositions, rotation or reopening a game can never make a listener react twice. A refused
 * command, a missing game or a game that ended on the way means no transition.
 */
class GameCommandRunner @Inject constructor(
    private val games: GameRepository,
    private val listeners: Set<@JvmSuppressWildcards GameTransitionListener>,
) {

    suspend fun <T, E> run(command: GameCommand, block: suspend () -> Outcome<T, E>): Outcome<T, E> {
        if (listeners.isEmpty()) return block()
        val before = games.findActiveGame() ?: return block()
        val beforeLog = games.findScoreLog(before.id)

        val result = block()
        if (result is Outcome.Failure) return result
        val after = games.findActiveGame()?.takeIf { it.id == before.id } ?: return result

        val transition = GameTransition(command, before, beforeLog, after)
        listeners.forEach { it.onTransition(transition) }
        return result
    }
}
