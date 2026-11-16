package io.github.khaledx5.dominoscore.domain.command

import io.github.khaledx5.dominoscore.domain.messages.ScoreLog
import io.github.khaledx5.dominoscore.domain.model.Game

/**
 * One command that went through, with the table right before and right after it. [beforeLog] is
 * the score log as it was before the command, for listeners that look back over the game.
 */
data class GameTransition(val command: GameCommand, val before: Game, val beforeLog: ScoreLog, val after: Game)

/** Reacts to a command that changed the table. Called once per command, never for a refused one. */
fun interface GameTransitionListener {
    suspend fun onTransition(transition: GameTransition)
}
