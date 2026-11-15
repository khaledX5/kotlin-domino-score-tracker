package io.github.khaledx5.dominoscore.domain.rules

import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.scores
import io.github.khaledx5.dominoscore.domain.target
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WinnerSelectionTest {

    private val target150 = target(150)

    @Test
    fun nobodyToConfirmBeforeAnyoneQualifies() {
        val selection = selectWinner(scores(149, 80), target150)

        assertEquals(WinnerSelection.NobodyQualified, selection)
        assertFalse(selection.allows(PlayerId(0)))
    }

    @Test
    fun suggestsTheOnlyQualifiedPlayer() {
        val selection = selectWinner(scores(40, 155, 90), target150) as WinnerSelection.Suggested

        assertEquals(1, selection.suggested.seat)
        assertEquals(listOf(1), selection.candidates.map { it.seat })
    }

    @Test
    fun suggestsTheHighestWhenSeveralQualify() {
        val selection = selectWinner(scores(150, 180, 160, 20), target150) as WinnerSelection.Suggested

        assertEquals(1, selection.suggested.seat)
        assertEquals(listOf(1, 2, 0), selection.candidates.map { it.seat })
    }

    @Test
    fun anyQualifiedPlayerMayStillBeConfirmed() {
        val selection = selectWinner(scores(150, 180, 20), target150)

        assertTrue(selection.allows(PlayerId(0)))
        assertTrue(selection.allows(PlayerId(1)))
        assertFalse(selection.allows(PlayerId(2)))
    }

    @Test
    fun aTieAtTheTopHasNoSuggestionAndOnlyTheTiedPlayersAreCandidates() {
        val selection = selectWinner(scores(170, 150, 170), target150)

        assertEquals(WinnerSelection.TieAtTop::class, selection::class)
        assertEquals(listOf(0, 2), selection.candidates.map { it.seat })
        assertFalse(selection.allows(PlayerId(1)))
    }

    @Test
    fun aTieBelowTheTargetDoesNotBlockTheSuggestion() {
        val selection = selectWinner(scores(100, 160, 100), target150) as WinnerSelection.Suggested

        assertEquals(1, selection.suggested.seat)
    }

    @Test
    fun aTieBetweenLowerQualifiedPlayersStillSuggestsTheLeader() {
        val selection = selectWinner(scores(155, 190, 155), target150) as WinnerSelection.Suggested

        assertEquals(1, selection.suggested.seat)
        assertEquals(listOf(1, 0, 2), selection.candidates.map { it.seat })
    }
}
