package io.github.khaledx5.dominoscore.domain.command

import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.ChangeTargetUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.domain.valueOrFail
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCommandRunnerTest {

    private val games = FakeGameRepository()
    private val seen = mutableListOf<GameTransition>()
    private val runner = GameCommandRunner(games, setOf(GameTransitionListener { seen += it }))
    private val recordScore = RecordScoreUseCase(games, fixedClock)
    private val changeTarget = ChangeTargetUseCase(games, fixedClock)

    private suspend fun start() {
        StartGameUseCase(games, fixedClock)(targetScore = 100, playerNames = listOf("Omar", "Mona")).valueOrFail()
    }

    @Test
    fun handsEveryListenerTheTableBeforeAndAfterTheCommandOnce() = runTest {
        start()
        val omar = games.activeGame().players.first()
        val command = GameCommand.Score(omar.id, ScoreOperationType.ADD, 25)

        runner.run(command) { recordScore(omar.id, ScoreOperationType.ADD, 25) }

        val transition = seen.single()
        assertEquals(command, transition.command)
        assertEquals(0, transition.before.player(omar.id)!!.score)
        assertEquals(25, transition.after.player(omar.id)!!.score)
        assertTrue(transition.beforeLog.operations.isEmpty())
    }

    @Test
    fun aRefusedCommandReachesNoListenerAndKeepsItsAnswer() = runTest {
        start()

        val result = runner.run(GameCommand.ChangeTarget) { changeTarget(100) }

        assertTrue(result is Outcome.Failure)
        assertTrue(seen.isEmpty())
    }

    @Test
    fun withoutAGameTheCommandStillRunsButNobodyIsCalled() = runTest {
        var ran = false

        runner.run(GameCommand.EndRound) {
            ran = true
            Outcome.Success(Unit)
        }

        assertTrue(ran)
        assertTrue(seen.isEmpty())
    }
}
