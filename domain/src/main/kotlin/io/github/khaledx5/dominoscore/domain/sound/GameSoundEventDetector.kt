package io.github.khaledx5.dominoscore.domain.sound

import io.github.khaledx5.dominoscore.domain.command.GameCommand
import io.github.khaledx5.dominoscore.domain.command.GameTransition
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.rules.isQualified
import io.github.khaledx5.dominoscore.domain.rules.rankPlayers

/**
 * Finds the sound event of one command, if any. Every event is a change from "not true before" to
 * "true after" (or, for the penalty, the operation itself), so nothing fires again while the table
 * just stays the same, and reopening a game replays nothing. Pure: no clock, no storage, no Android.
 */
class GameSoundEventDetector(private val rules: GameSoundRules = GameSoundRules()) {

    /** The highest priority event of [transition], or null when the command caused none. */
    fun detect(transition: GameTransition): GameSoundEvent? = detectAll(transition).minOrNull()

    /** Every event of [transition], in no particular order. */
    fun detectAll(transition: GameTransition): Set<GameSoundEvent> = buildSet {
        val (command, before, _, after) = transition
        if (qualifiedCount(before) < 2 && qualifiedCount(after) >= 2) add(GameSoundEvent.SECOND_QUALIFIED)
        if (!inFinalStretch(before) && inFinalStretch(after)) add(GameSoundEvent.FINAL_STRETCH)
        if (command is GameCommand.Score && lastPlayerScored(command, before, after)) {
            add(GameSoundEvent.LAST_PLAYER_SCORES)
        }
        if ((equalGroups(after) - equalGroups(before)).isNotEmpty()) add(GameSoundEvent.EQUAL_SCORES)
        if (command is GameCommand.Score && command.isLargePenalty()) add(GameSoundEvent.LARGE_PENALTY)
    }

    private fun qualifiedCount(game: Game): Int = game.players.count { it.isQualified(game.target) }

    /** The two best players, in the ranking's own order, are both within the last stretch but not there yet. */
    private fun inFinalStretch(game: Game): Boolean {
        if (game.players.size < 2) return false
        val stretch = (game.target.value - rules.finalStretchPoints) until game.target.value
        return rankPlayers(game.players).take(2).all { it.player.score in stretch }
    }

    /** Before, the scorer was the only one on 0 and everyone else was above 0; after an add, the scorer is above 0 too. */
    private fun lastPlayerScored(command: GameCommand.Score, before: Game, after: Game): Boolean {
        if (command.type != ScoreOperationType.ADD) return false
        val scorerBefore = before.player(command.playerId) ?: return false
        val scorerAfter = after.player(command.playerId) ?: return false
        val othersAboveZero = before.players.all { it.id == command.playerId || it.score > 0 }
        return scorerBefore.score == 0 && othersAboveZero && scorerAfter.score > 0
    }

    /** Each group of two or more players sharing a score above the threshold: the score and who is on it. */
    private fun equalGroups(game: Game): Set<Pair<Int, Set<PlayerId>>> = game.players
        .filter { it.score > rules.equalScoresAbove }
        .groupBy { it.score }
        .filterValues { it.size >= 2 }
        .map { (score, players) -> score to players.map(Player::id).toSet() }
        .toSet()

    private fun GameCommand.Score.isLargePenalty(): Boolean =
        type == ScoreOperationType.PENALTY && amount > rules.largePenaltyAbove
}
