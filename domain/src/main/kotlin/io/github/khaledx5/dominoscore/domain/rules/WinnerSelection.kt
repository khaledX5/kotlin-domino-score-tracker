package io.github.khaledx5.dominoscore.domain.rules

import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.TargetScore

/**
 * Who the table may confirm as the winner. The app only suggests: the table always confirms,
 * because house rules differ (some tables give the win to whoever crossed the target first).
 */
sealed interface WinnerSelection {
    /** Players the table may confirm, best first. */
    val candidates: List<Player>

    fun allows(playerId: PlayerId): Boolean = candidates.any { it.id == playerId }

    /** Nobody reached the target yet, so there is no winner to confirm. */
    data object NobodyQualified : WinnerSelection {
        override val candidates: List<Player> = emptyList()
    }

    /** One qualified player has the best score and is pre-selected; any qualified player may still be picked. */
    data class Suggested(val suggested: Player, override val candidates: List<Player>) : WinnerSelection

    /** Several qualified players share the best score: no suggestion, the table must pick one of them. */
    data class TieAtTop(override val candidates: List<Player>) : WinnerSelection
}

fun selectWinner(players: List<Player>, target: TargetScore): WinnerSelection {
    val qualified = rankPlayers(qualifiedPlayers(players, target))
    val leaders = qualified.filter { it.rank == 1 }.map { it.player }
    return when {
        leaders.isEmpty() -> WinnerSelection.NobodyQualified
        leaders.size > 1 -> WinnerSelection.TieAtTop(leaders)
        else -> WinnerSelection.Suggested(suggested = leaders.single(), candidates = qualified.map { it.player })
    }
}
