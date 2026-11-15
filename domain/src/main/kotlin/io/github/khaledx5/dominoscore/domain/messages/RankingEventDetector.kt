package io.github.khaledx5.dominoscore.domain.messages

import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.ALL_PLAYERS_NEGATIVE
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.ESCAPED_KOOZ
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.EXCEEDED_TARGET
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.EXTREMELY_NEGATIVE_SCORE
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.FIRST_TIME_KING
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.FIRST_TIME_KOOZ
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.FIRST_TO_LAST
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.GAINED_THREE_RANKS
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.GENERAL_HUMOR
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.KING_COMEBACK
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.KOOZ_COMEBACK
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.LAST_TO_FIRST
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.LOST_LONG_STANDING_FIRST
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.LOST_TWO_RANKS
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.MAJOR_COMEBACK
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.NEGATIVE_TO_POSITIVE
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.ONE_POINT_LOST_FIRST
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.OVERTAKEN_PLAYER
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.REACHED_TARGET
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.STILL_KING
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.STILL_KOOZ
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.TIED_FIRST
import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.rules.RankedPlayer
import io.github.khaledx5.dominoscore.domain.rules.rankPlayers

/** One player gets one message of [category]. */
data class MessageEvent(val playerId: PlayerId, val category: MessageCategory)

/** The table before and after something happened. Both sides are ranked with [rankPlayers]. */
data class TableChange(
    val before: List<Player>,
    val after: List<Player>,
    val targetBefore: TargetScore,
    val targetAfter: TargetScore,
    val trigger: MessageTrigger,
    /** The player whose score changed, for a score tap. */
    val actor: PlayerId? = null,
)

/**
 * Turns a change at the table into at most one message per player, the strongest one that really
 * applies. Score taps and target changes only speak to players whose standing changed; a round
 * end gives everyone a fresh message. Never ranks players itself: KING, KOOZ and ranks come from
 * [rankPlayers], so the same rules hold here as everywhere else.
 */
object RankingEventDetector {

    /** Holding first place for this many completed rounds in a row makes losing it a special event. */
    const val LONG_STANDING_ROUNDS = 3

    /** LAST_TO_FIRST and FIRST_TO_LAST need a middle to jump over. */
    private const val MIN_PLAYERS_FOR_JUMPS = 3

    /**
     * GAINED_THREE_RANKS fires on a climb of this many ranks. Three is impossible in practice: with at most
     * four players, rank 4 to rank 1 is always LAST_TO_FIRST, so the rocket marks a two-rank climb.
     */
    private const val ROCKET_RANKS = 2
    private const val TWO_RANKS = 2

    fun detect(change: TableChange, history: Map<PlayerId, TitleHistory>): List<MessageEvent> {
        val before = rankPlayers(change.before).associateBy { it.player.id }
        val after = rankPlayers(change.after).associateBy { it.player.id }
        val table = Table(change, before.values.toList(), after.values.toList())
        return change.after.mapNotNull { player ->
            val was = before[player.id] ?: return@mapNotNull null
            val now = after.getValue(player.id)
            val seen = history[player.id] ?: TitleHistory()
            categoriesFor(was, now, seen, table).strongest()?.let { MessageEvent(player.id, it) }
        }
    }

    private class Table(val change: TableChange, val before: List<RankedPlayer>, val after: List<RankedPlayer>) {
        val size = after.size
        val topScoreAfter = after.maxOf { it.player.score }
        val kingsBefore = before.count { it.isKing }
        val kingsAfter = after.count { it.isKing }
        val allNegativeNow = after.all { it.player.score < 0 } && !before.all { it.player.score < 0 }
        val roundEnd = change.trigger == MessageTrigger.ROUND_END

        fun scoreBefore(id: PlayerId) = before.first { it.player.id == id }.player.score

        fun scoreAfter(id: PlayerId) = after.first { it.player.id == id }.player.score
    }

    private fun categoriesFor(
        was: RankedPlayer,
        now: RankedPlayer,
        seen: TitleHistory,
        table: Table,
    ): List<MessageCategory> = buildList {
        addAll(specialEvents(was, now, seen, table))
        if (now.isKing && !was.isKing) add(if (seen.everKing) KING_COMEBACK else FIRST_TIME_KING)
        if (now.isKooz && !was.isKooz) add(if (seen.everKooz) KOOZ_COMEBACK else FIRST_TIME_KOOZ)
        if (table.roundEnd) {
            if (now.isKing && was.isKing) add(STILL_KING)
            if (now.isKooz && was.isKooz) add(STILL_KOOZ)
            if (isEmpty()) add(GENERAL_HUMOR)
        }
    }

    private fun specialEvents(was: RankedPlayer, now: RankedPlayer, seen: TitleHistory, table: Table) = buildList {
        val change = table.change
        val bigTable = table.size >= MIN_PLAYERS_FOR_JUMPS
        val scoreWas = was.player.score
        val score = now.player.score
        val lostKing = was.isKing && !now.isKing
        val gainedKing = now.isKing && !was.isKing

        if (bigTable && was.isKooz && now.isKing) add(LAST_TO_FIRST)
        if (bigTable && was.isKing && now.isKooz) add(FIRST_TO_LAST)
        if (gainedKing && seen.koozAtARoundEnd) add(MAJOR_COMEBACK)
        if (lostKing && seen.kingStreakRounds >= LONG_STANDING_ROUNDS) add(LOST_LONG_STANDING_FIRST)
        if (lostKing && table.topScoreAfter - score == 1) add(ONE_POINT_LOST_FIRST)

        val target = change.targetAfter.value
        val reached = scoreWas < change.targetBefore.value && score >= target
        if (reached && score >= target + target / 4) add(EXCEEDED_TARGET)
        if (reached) add(REACHED_TARGET)

        if (was.isKooz && !now.isKooz && !now.isKing) add(ESCAPED_KOOZ)
        if (scoreWas < 0 && score > 0) add(NEGATIVE_TO_POSITIVE)
        if (was.rank - now.rank >= ROCKET_RANKS) add(GAINED_THREE_RANKS)
        if (now.rank - was.rank >= TWO_RANKS) add(LOST_TWO_RANKS)
        if (now.isKing && table.kingsAfter >= 2 && !(was.isKing && table.kingsBefore >= 2)) add(TIED_FIRST)
        if (wasOvertaken(now.player.id, table)) add(OVERTAKEN_PLAYER)

        val extreme = -maxOf(target / 2, 1)
        if (score <= extreme && scoreWas > extreme) add(EXTREMELY_NEGATIVE_SCORE)
        if (table.allNegativeNow && !table.roundEnd && change.actor == now.player.id) add(ALL_PLAYERS_NEGATIVE)
    }

    /** Someone who was behind the player moved up and is now ahead of them. */
    private fun wasOvertaken(id: PlayerId, table: Table): Boolean {
        val mineBefore = table.scoreBefore(id)
        val mineAfter = table.scoreAfter(id)
        return table.after.any { other ->
            val otherId = other.player.id
            val otherBefore = table.scoreBefore(otherId)
            val otherAfter = table.scoreAfter(otherId)
            otherId != id && otherAfter > otherBefore && otherBefore < mineBefore && otherAfter > mineAfter
        }
    }
}
