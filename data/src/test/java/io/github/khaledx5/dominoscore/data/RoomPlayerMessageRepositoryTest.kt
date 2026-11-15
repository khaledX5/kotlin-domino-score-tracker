package io.github.khaledx5.dominoscore.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import io.github.khaledx5.dominoscore.data.local.DominoDatabase
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory
import io.github.khaledx5.dominoscore.domain.messages.MessageTrigger
import io.github.khaledx5.dominoscore.domain.messages.PlayerMessageRecord
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.model.getOrNull
import io.github.khaledx5.dominoscore.domain.repository.NewGame
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomPlayerMessageRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val t0 = Instant.parse("2026-11-15T18:00:00Z")
    private lateinit var database: DominoDatabase

    private fun open(name: String? = null): DominoDatabase {
        val builder = if (name == null) {
            Room.inMemoryDatabaseBuilder(context, DominoDatabase::class.java)
        } else {
            Room.databaseBuilder(context, DominoDatabase::class.java, name)
        }
        return builder.allowMainThreadQueries().build()
    }

    @Before
    fun setUp() {
        database = open()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun startGame(db: DominoDatabase = database): Game {
        val games = RoomGameRepository(db)
        games.startGame(
            NewGame(
                target = TargetScore.of(100).getOrNull()!!,
                playerNames = listOf("Omar", "Mona", "Ali").map { PlayerName.of(it).getOrNull()!! },
                startedAt = t0,
            ),
        )
        return games.findActiveGame()!!
    }

    private fun Game.record(seat: Int, messageId: String, category: MessageCategory, second: Long = 0) =
        PlayerMessageRecord(
            gameId = id,
            playerId = players[seat].id,
            roundId = currentRound!!.id,
            messageId = messageId,
            category = category,
            trigger = MessageTrigger.SCORE_CHANGE,
            createdAt = t0.plusSeconds(second),
        )

    @Test
    fun savesAndReadsTheHistoryInOrder() = runTest {
        val game = startGame()
        val repository = RoomPlayerMessageRepository(database)
        val first = game.record(0, "first_time_king_001", MessageCategory.FIRST_TIME_KING)
        val second = game.record(1, "first_time_kooz_004", MessageCategory.FIRST_TIME_KOOZ, second = 1)

        repository.save(listOf(first, second))

        assertEquals(listOf(first, second), repository.history(game.id))
    }

    @Test
    fun theLatestMessageIsKeptPerPlayer() = runTest {
        val game = startGame()
        val repository = RoomPlayerMessageRepository(database)

        repository.observeLatest(game.id).test {
            assertEquals(emptyMap<PlayerId, PlayerMessageRecord>(), awaitItem())

            repository.save(listOf(game.record(0, "first_time_king_001", MessageCategory.FIRST_TIME_KING)))
            awaitItem()
            val newer = game.record(0, "still_king_003", MessageCategory.STILL_KING, second = 5)
            val mona = game.record(1, "still_kooz_002", MessageCategory.STILL_KOOZ, second = 5)
            repository.save(listOf(newer, mona))

            assertEquals(mapOf(game.players[0].id to newer, game.players[1].id to mona), awaitItem())
        }
    }

    @Test
    fun theHistorySurvivesClosingTheAppAndOpeningItAgain() = runTest {
        val name = "restart-test.db"
        context.deleteDatabase(name)
        val firstRun = open(name)
        val game = startGame(firstRun)
        val record = game.record(2, "general_humor_007", MessageCategory.GENERAL_HUMOR)
        RoomPlayerMessageRepository(firstRun).save(listOf(record))
        firstRun.close()

        val secondRun = open(name)
        try {
            assertEquals(listOf(record), RoomPlayerMessageRepository(secondRun).history(game.id))
        } finally {
            secondRun.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun gamesKeepTheirOwnHistory() = runTest {
        val first = startGame()
        val repository = RoomPlayerMessageRepository(database)
        repository.save(listOf(first.record(0, "first_time_king_001", MessageCategory.FIRST_TIME_KING)))

        val second = startGame()

        assertEquals(emptyList<PlayerMessageRecord>(), repository.history(second.id))
        assertEquals(1, repository.history(first.id).size)
    }
}
