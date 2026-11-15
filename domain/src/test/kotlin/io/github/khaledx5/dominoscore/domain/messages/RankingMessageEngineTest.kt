package io.github.khaledx5.dominoscore.domain.messages

import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.ALL_PLAYERS_NEGATIVE
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.FIRST_TIME_KING
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.FIRST_TIME_KOOZ
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.GENERAL_HUMOR
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.REACHED_TARGET
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.STILL_KING
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.STILL_KOOZ
import io.github.khaledx5.dominoscore.domain.messages.MessageTrigger.ROUND_END
import io.github.khaledx5.dominoscore.domain.messages.MessageTrigger.SCORE_CHANGE
import io.github.khaledx5.dominoscore.domain.messages.MessageTrigger.TARGET_CHANGE
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.testing.FakePlayerMessageRepository
import io.github.khaledx5.dominoscore.domain.usecase.ChangeTargetUseCase
import io.github.khaledx5.dominoscore.domain.usecase.EndRoundUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.domain.valueOrFail
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingMessageEngineTest {

    private val games = FakeGameRepository()
    private val messages = FakePlayerMessageRepository()
    private var library = MessageLibrary(
        MessageCategory.entries.flatMap { category ->
            (1..4).map { RankingMessage("${category.name.lowercase()}_$it", category, "line $it") }
        },
    )
    private val engine =
        RankingMessageEngine(games, messages, { library }, RankingMessageSelector(Random(3)), fixedClock)

    private val startGame = StartGameUseCase(games, fixedClock)
    private val recordScore = RecordScoreUseCase(games, fixedClock)
    private val endRound = EndRoundUseCase(games, fixedClock)
    private val changeTarget = ChangeTargetUseCase(games, fixedClock)

    private suspend fun start(vararg names: String, target: Int = 100): List<Player> {
        startGame(targetScore = target, playerNames = names.toList(), replaceActiveGame = true).valueOrFail()
        return games.activeGame().players
    }

    private suspend fun score(player: Player, points: Int) = engine.react(SCORE_CHANGE, player.id) {
        val type = if (points >= 0) ScoreOperationType.ADD else ScoreOperationType.PENALTY
        recordScore(player.id, type, kotlin.math.abs(points))
    }

    private suspend fun endTheRound() = engine.react(ROUND_END) { endRound() }

    private fun categoriesOf(trigger: MessageTrigger) = messages.all.filter { it.trigger == trigger }
        .associate { it.playerId to it.category }

    @Test
    fun theFirstScoreCrownsAKingAndGivesTheOthersTheirBucket() = runTest {
        val (omar, mona, ali) = start("Omar", "Mona", "Ali")

        score(omar, 10)

        assertEquals(
            mapOf(omar.id to FIRST_TIME_KING, mona.id to FIRST_TIME_KOOZ, ali.id to FIRST_TIME_KOOZ),
            categoriesOf(SCORE_CHANGE),
        )
        // Two players in the same event get two different lines.
        val koozLines = messages.all.filter { it.category == FIRST_TIME_KOOZ }.map { it.messageId }
        assertEquals(2, koozLines.toSet().size)
    }

    @Test
    fun aScoreThatChangesNoStandingSavesNothing() = runTest {
        val (omar, _, _) = start("Omar", "Mona", "Ali")
        score(omar, 10)
        val saved = messages.all.size

        score(omar, 5)

        assertEquals(saved, messages.all.size)
    }

    @Test
    fun aRefusedCommandSavesNothingAndReturnsTheRefusal() = runTest {
        val (omar, _) = start("Omar", "Mona")

        val result = engine.react(SCORE_CHANGE, omar.id) { recordScore(omar.id, ScoreOperationType.ADD, 0) }

        assertTrue(result is Outcome.Failure)
        assertEquals(emptyList<PlayerMessageRecord>(), messages.all)
    }

    @Test
    fun everyPlayerGetsAFreshMessageWhenARoundEnds() = runTest {
        val (omar, mona, ali) = start("Omar", "Mona", "Ali")
        score(omar, 30)
        score(mona, 10)
        endTheRound()

        endTheRound()

        val round2 = messages.all.filter {
            it.trigger == ROUND_END
        }.takeLast(3).associate { it.playerId to it.category }
        assertEquals(mapOf(omar.id to STILL_KING, mona.id to GENERAL_HUMOR, ali.id to STILL_KOOZ), round2)
    }

    @Test
    fun stillKingTwiceInARowIsTwoDifferentLines() = runTest {
        val (omar, _) = start("Omar", "Mona")
        score(omar, 30)
        endTheRound()
        endTheRound()
        endTheRound()

        val lines = messages.all.filter { it.playerId == omar.id }.map { it.messageId }
        lines.zipWithNext().forEach { (previous, next) -> assertNotEquals(previous, next) }
    }

    @Test
    fun playersKeepIndependentHistories() = runTest {
        val (omar, mona) = start("Omar", "Mona")
        score(omar, 30)
        repeat(3) { endTheRound() }

        val omarsLines = messages.all.filter { it.playerId == omar.id }
        val monasLines = messages.all.filter { it.playerId == mona.id }
        assertTrue(omarsLines.all { it.category.tone != MessageTone.KOOZ })
        assertTrue(monasLines.all { it.category.tone != MessageTone.KING })
    }

    @Test
    fun loweringTheTargetSoSomeoneReachesItIsAnEvent() = runTest {
        val (omar, _, _) = start("Omar", "Mona", "Ali")
        score(omar, 60)
        val before = messages.all.size

        engine.react(TARGET_CHANGE) { changeTarget(50) }

        val added = messages.all.drop(before)
        assertEquals(listOf(omar.id to REACHED_TARGET), added.map { it.playerId to it.category })
    }

    @Test
    fun theTapThatPutsEveryoneBelowZeroGoesToWhoeverMadeIt() = runTest {
        val (omar, mona) = start("Omar", "Mona")
        score(omar, -10)
        val before = messages.all.size

        score(mona, -5)

        assertEquals(ALL_PLAYERS_NEGATIVE, messages.all.drop(before).single { it.playerId == mona.id }.category)
    }

    @Test
    fun anEmptyLibraryKeepsTheGameGoingWithoutMessages() = runTest {
        library = MessageLibrary.EMPTY
        val (omar, _) = start("Omar", "Mona")

        val result = score(omar, 10)

        assertTrue(result is Outcome.Success)
        assertEquals(10, games.activeGame().players.first().score)
        assertEquals(emptyList<PlayerMessageRecord>(), messages.all)
    }

    @Test
    fun worksWithTwoThreeAndFourPlayers() = runTest {
        listOf(listOf("A", "B"), listOf("A", "B", "C"), listOf("A", "B", "C", "D")).forEach { names ->
            val players = start(*names.toTypedArray())
            val before = messages.all.size

            score(players.last(), 20)
            endTheRound()

            val added = messages.all.drop(before)
            // The tap speaks to everyone (one new KING, the rest KOOZ), and so does the round end.
            assertEquals(names.size * 2, added.size)
            assertTrue(added.all { record -> players.any { it.id == record.playerId } })
        }
    }
}
