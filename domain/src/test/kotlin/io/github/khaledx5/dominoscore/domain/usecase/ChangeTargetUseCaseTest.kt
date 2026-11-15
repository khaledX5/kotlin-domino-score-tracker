package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.game
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.RangeError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChangeTargetUseCaseTest {

    private val repository = FakeGameRepository()
    private val changeTarget = ChangeTargetUseCase(repository, fixedClock)

    @Test
    fun savesTheNewTarget() = runTest {
        repository.seed(game(target = 150))

        assertEquals(200, (changeTarget(200) as Outcome.Success).value.value)
        assertEquals(200, repository.activeGame().target.value)
    }

    @Test
    fun rejectsTargetsOutsideTheAllowedRange() = runTest {
        repository.seed(game())

        assertEquals(Outcome.Failure(InvalidTarget(RangeError.TOO_LOW)), changeTarget(0))
        assertEquals(Outcome.Failure(InvalidTarget(RangeError.TOO_HIGH)), changeTarget(100_000))
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun rejectsTheSameTargetSoNoEmptyEventIsWritten() = runTest {
        repository.seed(game(target = 150))

        assertEquals(Outcome.Failure(TargetUnchanged), changeTarget(150))
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun failsWithoutAnActiveGame() = runTest {
        assertEquals(Outcome.Failure(NoActiveGame), changeTarget(200))
    }
}
