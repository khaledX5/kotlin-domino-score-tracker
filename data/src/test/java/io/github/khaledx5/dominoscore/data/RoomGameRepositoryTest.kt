package io.github.khaledx5.dominoscore.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import io.github.khaledx5.dominoscore.data.local.DominoDatabase
import io.github.khaledx5.dominoscore.data.local.GameDao
import io.github.khaledx5.dominoscore.data.local.entity.GameEntity
import io.github.khaledx5.dominoscore.data.local.entity.GameEventType
import io.github.khaledx5.dominoscore.data.local.entity.PlayerEntity
import io.github.khaledx5.dominoscore.data.local.entity.RoundEntity
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.RoundId
import io.github.khaledx5.dominoscore.domain.model.RoundStatus
import io.github.khaledx5.dominoscore.domain.model.ScoreAmount
import io.github.khaledx5.dominoscore.domain.model.ScoreOperation
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.model.getOrNull
import io.github.khaledx5.dominoscore.domain.repository.NewGame
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomGameRepositoryTest {

    private lateinit var database: DominoDatabase
    private lateinit var dao: GameDao
    private lateinit var repository: RoomGameRepository

    private val t0 = Instant.parse("2026-11-15T18:00:00Z")
    private val t1 = t0.plusSeconds(60)

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(),
                DominoDatabase::class.java,
            )
                .allowMainThreadQueries()
                .build()
        dao = database.gameDao()
        repository = RoomGameRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun start(vararg names: String, target: Int = 150): Game {
        repository.startGame(
            NewGame(
                target = TargetScore.of(target).getOrNull()!!,
                playerNames = names.map { PlayerName.of(it).getOrNull()!! },
                startedAt = t0,
            ),
        )
        return repository.findActiveGame()!!
    }

    private suspend fun assertIllegalState(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected an IllegalStateException")
        } catch (_: IllegalStateException) {
            // Expected: the repository refuses a write that breaks its preconditions.
        }
    }

    private fun Game.round(): RoundId = currentRound!!.id

    private fun Game.seat(seat: Int): PlayerId = players[seat].id

    private fun add(game: Game, seat: Int, amount: Int, previous: Int = 0) = ScoreOperation(
        roundId = game.round(),
        playerId = game.seat(seat),
        type = ScoreOperationType.ADD,
        amount = ScoreAmount.of(amount).getOrNull()!!,
        previousScore = previous,
        newScore = previous + amount,
        createdAt = t1,
    )

    @Test
    fun startGameCreatesPlayersInSeatOrderAndRoundOne() = runTest {
        val game = start("Omar", "Mona", "Ali", target = 200)

        assertEquals(GameStatus.ACTIVE, game.status)
        assertEquals(200, game.target.value)
        assertEquals(listOf("Omar", "Mona", "Ali"), game.players.map { it.name.value })
        assertEquals(listOf(0, 1, 2), game.players.map { it.seat })
        assertEquals(listOf(0, 0, 0), game.players.map { it.score })
        assertEquals(1, game.currentRound?.number)
        assertEquals(t0, game.startedAt)
    }

    @Test
    fun startingAnotherGameAbandonsTheActiveOne() = runTest {
        val first = start("Omar", "Mona")
        val second = start("Ali", "Sara")

        assertEquals(GameStatus.ABANDONED, dao.findGameRow(first.id.value)?.status)
        assertEquals(second.id, repository.findActiveGame()?.id)
    }

    @Test
    fun recordScoreAppendsTheOperationAndUpdatesTheScore() = runTest {
        val game = start("Omar", "Mona")

        repository.recordScore(game.id, add(game, seat = 1, amount = 35))

        assertEquals(35, repository.findActiveGame()?.players?.get(1)?.score)
        val stored = dao.findScoreOperations(game.id.value).single()
        assertEquals(0, stored.previousScore)
        assertEquals(35, stored.newScore)
        assertEquals(game.round().value, stored.roundId)
    }

    @Test
    fun recordScoreRejectsAPlayerOfAnotherGame() = runTest {
        val old = start("Omar", "Mona")
        val game = start("Ali", "Sara")

        assertIllegalState { repository.recordScore(game.id, add(old, seat = 0, amount = 5)) }
        assertEquals(emptyList<Any>(), dao.findScoreOperations(game.id.value))
    }

    @Test
    fun changeTargetSavesTheValueAndAnEventWithOldAndNew() = runTest {
        val game = start("Omar", "Mona", target = 150)

        repository.changeTarget(game.id, TargetScore.of(250).getOrNull()!!, t1)

        assertEquals(250, repository.findActiveGame()?.target?.value)
        val event = dao.findEvents(game.id.value).single()
        assertEquals(GameEventType.TARGET_CHANGED, event.type)
        assertEquals("150", event.oldValue)
        assertEquals("250", event.newValue)
        assertEquals(game.round().value, event.roundId)
    }

    @Test
    fun completeRoundClosesItAndOpensTheNextOneKeepingScores() = runTest {
        val game = start("Omar", "Mona")
        repository.recordScore(game.id, add(game, seat = 0, amount = 40))

        repository.completeRound(game.id, game.round(), t1)

        val details = dao.findGame(game.id.value)!!
        assertEquals(
            listOf(RoundStatus.COMPLETED, RoundStatus.IN_PROGRESS),
            details.rounds.sortedBy {
                it.number
            }.map { it.status },
        )
        assertEquals(t1, details.rounds.single { it.number == 1 }.completedAt)
        assertEquals(2, repository.findActiveGame()?.currentRound?.number)
        assertEquals(40, repository.findActiveGame()?.players?.first()?.score)
        assertEquals(listOf(GameEventType.ROUND_COMPLETED), dao.findEvents(game.id.value).map { it.type })
    }

    @Test
    fun completeRoundRejectsARoundThatIsNoLongerInProgress() = runTest {
        val game = start("Omar", "Mona")
        repository.completeRound(game.id, game.round(), t1)

        assertIllegalState { repository.completeRound(game.id, game.round(), t1) }
        assertEquals(2, dao.findGame(game.id.value)!!.rounds.size)
    }

    @Test
    fun completeGameSavesTheWinnerAndClosesTheLastRound() = runTest {
        val game = start("Omar", "Mona")
        val winner = game.seat(1)

        repository.completeGame(game.id, winner, t1)

        val row = dao.findGameRow(game.id.value)!!
        assertEquals(GameStatus.COMPLETED, row.status)
        assertEquals(winner.value, row.winnerPlayerId)
        assertEquals(t1, row.completedAt)
        assertEquals(RoundStatus.COMPLETED, dao.findGame(game.id.value)!!.rounds.single().status)
        assertEquals(
            listOf(GameEventType.WINNER_CONFIRMED, GameEventType.GAME_COMPLETED),
            dao.findEvents(game.id.value).map { it.type },
        )
        assertNull(repository.findActiveGame())
    }

    @Test
    fun aFailedCompleteGameRollsBackEveryStep() = runTest {
        // A finished game that still has an open round: completing it must fail without touching the round.
        val gameId = dao.insertGame(GameEntity(targetScore = 100, status = GameStatus.COMPLETED, startedAt = t0))
        dao.insertPlayers(listOf(PlayerEntity(gameId = gameId, seat = 0, name = "Omar", score = 120, colorIndex = 0)))
        dao.insertRound(RoundEntity(gameId = gameId, number = 1, status = RoundStatus.IN_PROGRESS, startedAt = t0))
        val playerId = dao.findGame(gameId)!!.players.single().id

        assertIllegalState { repository.completeGame(GameId(gameId), PlayerId(playerId), t1) }

        assertEquals(RoundStatus.IN_PROGRESS, dao.findGame(gameId)!!.rounds.single().status)
        assertEquals(emptyList<Any>(), dao.findEvents(gameId))
    }

    @Test
    fun renameKeepsThePlayerIdAndRecordsTheOldName() = runTest {
        val game = start("Omar", "Mona")
        val omar = game.seat(0)

        repository.renamePlayer(game.id, omar, PlayerName.of("Omar K.").getOrNull()!!, t1)

        assertEquals("Omar K.", repository.findActiveGame()?.player(omar)?.name?.value)
        val event = dao.findEvents(game.id.value).single()
        assertEquals(GameEventType.PLAYER_RENAMED, event.type)
        assertEquals("Omar", event.oldValue)
        assertEquals("Omar K.", event.newValue)
        assertEquals(omar.value, event.playerId)
    }

    @Test
    fun observeActiveGameEmitsAfterEveryWriteAndNullWhenItEnds() = runTest {
        repository.observeActiveGame().test {
            assertNull(awaitItem())

            val game = start("Omar", "Mona")
            assertEquals(game.id, awaitItem()?.id)

            repository.recordScore(game.id, add(game, seat = 0, amount = 20))
            assertEquals(20, awaitItem()?.players?.first()?.score)

            repository.completeGame(game.id, game.seat(0), t1)
            assertNull(awaitItem())
        }
    }

    @Test
    fun scoresBelowZeroAreStoredAsIs() = runTest {
        val game = start("Omar", "Mona")
        val penalty = ScoreOperation(
            roundId = game.round(),
            playerId = game.seat(0),
            type = ScoreOperationType.PENALTY,
            amount = ScoreAmount.of(30).getOrNull()!!,
            previousScore = 0,
            newScore = -30,
            createdAt = t1,
        )

        repository.recordScore(game.id, penalty)

        assertEquals(-30, repository.findActiveGame()?.players?.first()?.score)
        assertEquals(-30, dao.findScoreOperations(game.id.value).single().newScore)
    }

    @Test
    fun movesComeNewestFirstWithTheirRoundNumber() = runTest {
        repository.startGame(
            NewGame(
                TargetScore.of(100).getOrNull()!!,
                listOf("Omar", "Mona").map {
                    PlayerName.of(it).getOrNull()!!
                },
                t0,
            ),
        )
        val game = repository.findActiveGame()!!
        val (omar, mona) = game.players
        val round1 = game.currentRound!!.id
        repository.recordScore(game.id, operation(round1, omar.id, ScoreOperationType.ADD, 20, 0, 20))
        repository.completeRound(game.id, round1, t1)
        val round2 = repository.findActiveGame()!!.currentRound!!.id
        repository.recordScore(game.id, operation(round2, mona.id, ScoreOperationType.PENALTY, 5, 0, -5))

        repository.observeMoves(game.id).test {
            val moves = awaitItem()
            assertEquals(listOf(2 to -5, 1 to 20), moves.map { it.roundNumber to it.points })
            assertEquals(listOf(mona.id, omar.id), moves.map { it.playerId })
        }
    }

    private fun operation(
        round: RoundId,
        player: PlayerId,
        type: ScoreOperationType,
        amount: Int,
        previous: Int,
        next: Int,
    ) = ScoreOperation(round, player, type, ScoreAmount.of(amount).getOrNull()!!, previous, next, t1)
}
