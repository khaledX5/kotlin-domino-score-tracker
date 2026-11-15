package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType.ADD
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType.PENALTY
import io.github.khaledx5.dominoscore.domain.rules.GameSnapshot
import io.github.khaledx5.dominoscore.domain.rules.PrimaryAction
import io.github.khaledx5.dominoscore.domain.rules.WinnerSelection
import io.github.khaledx5.dominoscore.domain.valueOrFail
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Whole games played through the use cases, the way a table would use the app. */
class GameScenarioTest {

    private val repository = FakeGameRepository()
    private val startGame = StartGameUseCase(repository, fixedClock)
    private val recordScore = RecordScoreUseCase(repository, fixedClock)
    private val changeTarget = ChangeTargetUseCase(repository, fixedClock)
    private val endRound = EndRoundUseCase(repository, fixedClock)
    private val confirmWinner = ConfirmWinnerUseCase(repository, fixedClock)
    private val renamePlayer = RenamePlayerUseCase(repository, fixedClock)

    private fun id(name: String): PlayerId = repository.activeGame().players.single { it.name.value == name }.id

    private suspend fun score(name: String, type: ScoreOperationType, amount: Int) {
        recordScore(id(name), type, amount).valueOrFail()
    }

    private fun snapshot(): GameSnapshot = GameSnapshot.of(repository.activeGame())

    private fun standings(): List<Pair<Int, String>> = snapshot().ranking.map { it.rank to it.player.name.value }

    @Test
    fun raisingTheTargetMidGameThenSettlingATieAtTheTop() = runTest {
        startGame(targetScore = 150, playerNames = listOf("Omar", "Mona", "Ali", "Sara")).valueOrFail()

        // Round 1: nobody reaches the target.
        score("Omar", ADD, 60)
        score("Mona", ADD, 45)
        score("Ali", ADD, 45)
        assertEquals(listOf(1 to "Omar", 2 to "Mona", 2 to "Ali", 4 to "Sara"), standings())
        assertEquals(Outcome.Success(1), endRound())

        // Round 2: Omar qualifies, so the round can no longer be ended.
        score("Omar", ADD, 95)
        score("Sara", ADD, 30)
        score("Ali", PENALTY, 10)
        assertEquals(PrimaryAction.END_GAME, snapshot().primaryAction)
        assertEquals(Outcome.Failure(PlayerQualified), endRound())

        // The table decides to play on to 200: the round can be ended again.
        changeTarget(200).valueOrFail()
        assertEquals(PrimaryAction.END_ROUND, snapshot().primaryAction)
        assertEquals(Outcome.Success(2), endRound())

        // Round 3: Omar and Mona finish on the same score above the target.
        score("Omar", ADD, 50)
        score("Mona", ADD, 160)
        val selection = snapshot().winnerSelection
        assertEquals(WinnerSelection.TieAtTop::class, selection::class)
        assertEquals(listOf("Omar", "Mona"), selection.candidates.map { it.name.value })

        val ali = id("Ali")
        val mona = id("Mona")
        assertEquals(Outcome.Failure(NotACandidate), confirmWinner(ali))
        assertEquals(Outcome.Success(Unit), confirmWinner(mona))

        val finished = repository.allGames().single()
        assertEquals(GameStatus.COMPLETED, finished.status)
        assertEquals(mona, finished.winnerId)
        assertEquals(listOf(205, 205, 35, 30), finished.players.map { it.score })
        assertEquals(listOf(1, 2, 3), repository.completedRounds.map { it.number })
        assertEquals(8, repository.operations.size)
        assertNull(repository.findActiveGame())
    }

    @Test
    fun aPenaltyAfterQualifyingReopensTheRound() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona")).valueOrFail()

        score("Omar", ADD, 100)
        assertEquals(PrimaryAction.END_GAME, snapshot().primaryAction)

        score("Omar", PENALTY, 15)
        assertEquals(PrimaryAction.END_ROUND, snapshot().primaryAction)
        assertEquals(WinnerSelection.NobodyQualified, snapshot().winnerSelection)
        assertEquals(Outcome.Success(1), endRound())
    }

    @Test
    fun loweringTheTargetCanEndTheGameAtOnce() = runTest {
        startGame(targetScore = 300, playerNames = listOf("Omar", "Mona", "Ali")).valueOrFail()
        score("Omar", ADD, 120)
        score("Ali", ADD, 180)

        changeTarget(150).valueOrFail()

        val selection = snapshot().winnerSelection as WinnerSelection.Suggested
        assertEquals("Ali", selection.suggested.name.value)
        assertEquals(Outcome.Success(Unit), confirmWinner(selection.suggested.id))
    }

    @Test
    fun renamingMidGameKeepsTheScoreAndTheHistory() = runTest {
        startGame(targetScore = 150, playerNames = listOf("Omar", "Mona")).valueOrFail()
        score("Omar", ADD, 40)
        val omar = id("Omar")

        renamePlayer(omar, "Omar K.").valueOrFail()
        score("Omar K.", ADD, 20)

        assertEquals(60, repository.activeGame().player(omar)?.score)
        assertEquals(listOf(omar, omar), repository.operations.map { it.playerId })
    }

    @Test
    fun replacingAGameKeepsTheAbandonedOneUntouched() = runTest {
        val first = startGame(targetScore = 150, playerNames = listOf("Omar", "Mona")).valueOrFail()
        score("Omar", ADD, 70)

        assertEquals(
            Outcome.Failure(ActiveGameExists),
            startGame(targetScore = 200, playerNames = listOf("Ali", "Sara")),
        )
        startGame(targetScore = 200, playerNames = listOf("Ali", "Sara"), replaceActiveGame = true).valueOrFail()

        val abandoned = repository.game(first)
        assertEquals(GameStatus.ABANDONED, abandoned.status)
        assertEquals(70, abandoned.players.first().score)
        assertEquals(listOf("Ali", "Sara"), repository.activeGame().players.map { it.name.value })
        assertEquals(listOf(0, 0), repository.activeGame().players.map { it.score })
    }
}
