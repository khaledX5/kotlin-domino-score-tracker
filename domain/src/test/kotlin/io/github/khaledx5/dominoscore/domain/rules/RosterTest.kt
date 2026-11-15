package io.github.khaledx5.dominoscore.domain.rules

import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerNameError
import io.github.khaledx5.dominoscore.domain.valueOrFail
import org.junit.Assert.assertEquals
import org.junit.Test

class RosterTest {

    private fun issuesOf(vararg names: String) = (validateRoster(names.toList()) as Outcome.Failure).error

    @Test
    fun acceptsTwoToFourValidNamesTrimmedAndInSeatOrder() {
        val names = validateRoster(listOf(" Omar ", "Mona", "Ali", "Sara")).valueOrFail()

        assertEquals(listOf("Omar", "Mona", "Ali", "Sara"), names.map { it.value })
    }

    @Test
    fun rejectsOneOrFivePlayers() {
        assertEquals(listOf(RosterIssue.WrongPlayerCount(1)), issuesOf("Omar"))
        assertEquals(listOf(RosterIssue.WrongPlayerCount(5)), issuesOf("A", "B", "C", "D", "E"))
    }

    @Test
    fun reportsEveryInvalidNameWithItsSeat() {
        val issues = issuesOf("Omar", "  ", "x".repeat(21))

        assertEquals(
            listOf(
                RosterIssue.InvalidName(seat = 1, error = PlayerNameError.BLANK),
                RosterIssue.InvalidName(seat = 2, error = PlayerNameError.TOO_LONG),
            ),
            issues,
        )
    }

    @Test
    fun blocksDuplicateNamesIgnoringCaseAndSpaces() {
        assertEquals(listOf(RosterIssue.DuplicateName(listOf(0, 2))), issuesOf("Omar", "Mona", " omar"))
    }

    @Test
    fun reportsEachGroupOfDuplicatesSeparately() {
        assertEquals(
            listOf(RosterIssue.DuplicateName(listOf(0, 2)), RosterIssue.DuplicateName(listOf(1, 3))),
            issuesOf("Ali", "Mona", "ALI", "mona"),
        )
    }
}
