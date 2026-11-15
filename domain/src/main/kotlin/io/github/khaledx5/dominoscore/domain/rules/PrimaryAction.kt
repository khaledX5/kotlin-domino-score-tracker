package io.github.khaledx5.dominoscore.domain.rules

import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.TargetScore

/**
 * The single main button of the game screen. Once anyone is qualified the round can no longer
 * be ended: the table either confirms a winner or raises the target.
 */
enum class PrimaryAction {
    END_ROUND,
    END_GAME,
}

/** Derived from the current score every time and never stored, so penalties and target changes apply at once. */
fun Player.isQualified(target: TargetScore): Boolean = score >= target.value

fun qualifiedPlayers(players: List<Player>, target: TargetScore): List<Player> = players.filter {
    it.isQualified(target)
}

fun primaryActionFor(players: List<Player>, target: TargetScore): PrimaryAction =
    if (players.any { it.isQualified(target) }) PrimaryAction.END_GAME else PrimaryAction.END_ROUND
