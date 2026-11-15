package io.github.khaledx5.dominoscore.domain.rules

import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.PlayerNameError

/** One problem in the list of names typed on the setup screen. Seats are 0-based list positions. */
sealed interface RosterIssue {
    data class WrongPlayerCount(val count: Int) : RosterIssue

    data class InvalidName(val seat: Int, val error: PlayerNameError) : RosterIssue

    /** The same name (ignoring case) is used on all these seats. */
    data class DuplicateName(val seats: List<Int>) : RosterIssue
}

/** Returns every issue at once, so the setup screen can mark all wrong fields together. */
fun validateRoster(rawNames: List<String>): Outcome<List<PlayerName>, List<RosterIssue>> {
    val parsed = rawNames.map(PlayerName::of)
    val validNames = parsed.withIndex().mapNotNull { (seat, outcome) ->
        (outcome as? Outcome.Success)?.let { seat to it.value }
    }
    val issues = buildList {
        if (rawNames.size !in Game.PLAYER_COUNT) add(RosterIssue.WrongPlayerCount(rawNames.size))
        parsed.forEachIndexed { seat, outcome ->
            if (outcome is Outcome.Failure) add(RosterIssue.InvalidName(seat, outcome.error))
        }
        addAll(duplicates(validNames))
    }
    return if (issues.isEmpty()) Outcome.Success(validNames.map { it.second }) else Outcome.Failure(issues)
}

private fun duplicates(names: List<Pair<Int, PlayerName>>): List<RosterIssue.DuplicateName> = names
    .groupBy(keySelector = { (_, name) -> name.value.lowercase() }, valueTransform = { (seat, _) -> seat })
    .values
    .filter { seats -> seats.size > 1 }
    .map(RosterIssue::DuplicateName)
