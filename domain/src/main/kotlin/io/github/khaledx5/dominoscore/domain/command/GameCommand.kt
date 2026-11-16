package io.github.khaledx5.dominoscore.domain.command

import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType

/** A change to the table that the screen asked for. Listeners learn what was asked, not only what changed. */
sealed interface GameCommand {
    /** One score for one player, as typed: [amount] is the positive number entered, [type] says add or penalty. */
    data class Score(val playerId: PlayerId, val type: ScoreOperationType, val amount: Int) : GameCommand

    data object ChangeTarget : GameCommand

    data object EndRound : GameCommand
}
