package io.github.khaledx5.dominoscore.domain.history

import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.ConfirmWinnerUseCase
import io.github.khaledx5.dominoscore.domain.usecase.EndRoundUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.domain.valueOrFail
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GameHistoryTest {

    private val games = FakeGameRepository()
    private val startGame = StartGameUseCase(games, fixedClock)
    private val recordScore = RecordScoreUseCase(games, fixedClock)
    private val endRound = EndRoundUseCase(games, fixedClock)

    @Test
    fun roundsShowEachPlayersPointsAndRunningTotals() = runTest {
        val gameId = startGame(50, listOf("Omar", "Mona")).valueOrFail()
        val (omar, mona) = games.activeGame().players
        recordScore(omar.id, ScoreOperationType.ADD, 20)
        recordScore(mona.id, ScoreOperationType.PENALTY, 5)
        endRound()
        endRound() // a round nobody scored in
        recordScore(omar.id, ScoreOperationType.ADD, 40)
        ConfirmWinnerUseCase(games, fixedClock)(omar.id)

        val rounds = GetGameDetailUseCase(games)(gameId)!!.rounds

        assertEquals(listOf(1, 2, 3), rounds.map { it.number })
        assertEquals(listOf(20, 0, 40), rounds.map { it.pointsByPlayer.getValue(omar.id) })
        assertEquals(listOf(-5, 0, 0), rounds.map { it.pointsByPlayer.getValue(mona.id) })
        assertEquals(listOf(20, 20, 60), rounds.map { it.totalsAfter.getValue(omar.id) })
        assertEquals(listOf(true, true, true), rounds.map { it.completed })
    }

    @Test
    fun anAbandonedGameEndsWithItsUnfinishedRound() = runTest {
        val gameId = startGame(50, listOf("Omar", "Mona")).valueOrFail()
        val omar = games.activeGame().players.first()
        endRound()
        recordScore(omar.id, ScoreOperationType.ADD, 15)
        startGame(50, listOf("Ali", "Sara"), replaceActiveGame = true)

        val rounds = GetGameDetailUseCase(games)(gameId)!!.rounds

        assertEquals(listOf(1 to true, 2 to false), rounds.map { it.number to it.completed })
        assertEquals(15, rounds.last().totalsAfter.getValue(omar.id))
    }
}
