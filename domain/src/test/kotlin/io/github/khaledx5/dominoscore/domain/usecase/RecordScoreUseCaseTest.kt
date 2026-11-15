package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.T0
import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.game
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.RangeError
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType.ADD
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType.PENALTY
import io.github.khaledx5.dominoscore.domain.scores
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.valueOrFail
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordScoreUseCaseTest {

    private val repository = FakeGameRepository()
    private val recordScore = RecordScoreUseCase(repository, fixedClock)

    private val first = PlayerId(0)

    @Test
    fun addSavesTheOperationAndTheNewScore() = runTest {
        repository.seed(game(scores(40, 10)))

        val operation = recordScore(first, ADD, 25).valueOrFail()

        assertEquals(40, operation.previousScore)
        assertEquals(65, operation.newScore)
        assertEquals(repository.activeGame().currentRound?.id, operation.roundId)
        assertEquals(T0, operation.createdAt)
        assertEquals(listOf(operation), repository.operations)
        assertEquals(65, repository.activeGame().player(first)?.score)
    }

    @Test
    fun penaltyCanTakeAPlayerBelowZero() = runTest {
        repository.seed(game(scores(10, 0)))

        recordScore(first, PENALTY, 30).valueOrFail()

        assertEquals(-20, repository.activeGame().player(first)?.score)
    }

    @Test
    fun rejectsAZeroAmount() = runTest {
        repository.seed(game())

        assertEquals(Outcome.Failure(InvalidAmount(RangeError.TOO_LOW)), recordScore(first, ADD, 0))
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun rejectsAScoreThatWouldOverflow() = runTest {
        repository.seed(game(scores(Int.MAX_VALUE - 1, 0)))

        assertEquals(Outcome.Failure(ScoreLimitReached), recordScore(first, ADD, 2))
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun rejectsAPlayerFromAnotherGame() = runTest {
        repository.seed(game())

        assertEquals(Outcome.Failure(UnknownPlayer), recordScore(PlayerId(42), ADD, 5))
    }

    @Test
    fun failsWithoutAnActiveGame() = runTest {
        assertEquals(Outcome.Failure(NoActiveGame), recordScore(first, ADD, 5))
    }
}
