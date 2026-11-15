package io.github.khaledx5.dominoscore.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.khaledx5.dominoscore.data.local.DominoDatabase
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory
import io.github.khaledx5.dominoscore.domain.messages.MessageTrigger
import io.github.khaledx5.dominoscore.domain.messages.PlayerMessageRecord
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.model.getOrNull
import io.github.khaledx5.dominoscore.domain.repository.NewGame
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A phone that already has an older schema must keep every game after updating the app. */
@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = context.getDatabasePath(NAME),
        driver = AndroidSQLiteDriver(),
        databaseClass = DominoDatabase::class,
    )
    private val startedAt = Instant.parse("2026-11-15T18:00:00Z").toEpochMilli()

    @After
    fun tearDown() {
        context.deleteDatabase(NAME)
    }

    @Test
    fun version1GamesSurviveTheMigrationToVersion2() = runTest {
        helper.createDatabase(1).apply {
            execSQL("INSERT INTO games (id, target_score, status, started_at) VALUES (1, 150, 'ACTIVE', $startedAt)")
            execSQL(
                "INSERT INTO players (id, game_id, seat, name, score, color_index) VALUES " +
                    "(10, 1, 0, 'Omar', 40, 0), (11, 1, 1, 'Mona', -5, 1)",
            )
            execSQL(
                "INSERT INTO rounds (id, game_id, number, status, started_at) " +
                    "VALUES (20, 1, 1, 'IN_PROGRESS', $startedAt)",
            )
            close()
        }

        // Checks the migrated schema against the exported 2.json, including the new table.
        helper.runMigrationsAndValidate(2).close()

        val database = openWithRoom()
        try {
            val game = RoomGameRepository(database).findActiveGame()!!
            assertEquals(listOf("Omar" to 40, "Mona" to -5), game.players.map { it.name.value to it.score })

            val messages = RoomPlayerMessageRepository(database)
            val record = PlayerMessageRecord(
                gameId = game.id,
                playerId = game.players[0].id,
                roundId = game.currentRound!!.id,
                messageId = "first_time_king_001",
                category = MessageCategory.FIRST_TIME_KING,
                trigger = MessageTrigger.SCORE_CHANGE,
                createdAt = Instant.ofEpochMilli(startedAt),
            )
            messages.save(listOf(record))
            assertEquals(listOf(record), messages.history(game.id))
        } finally {
            database.close()
        }
    }

    @Test
    fun version2PlayersGetProfilesByNameAndCompletedGamesGetFinalStandings() = runTest {
        helper.createDatabase(2).apply {
            execSQL(
                "INSERT INTO games (id, target_score, status, winner_player_id, started_at, completed_at) VALUES " +
                    "(1, 100, 'COMPLETED', 11, $startedAt, ${startedAt + 1}), " +
                    "(2, 100, 'ABANDONED', NULL, ${startedAt + 2}, NULL), " +
                    "(3, 100, 'ACTIVE', NULL, ${startedAt + 3}, NULL)",
            )
            execSQL(
                "INSERT INTO players (id, game_id, seat, name, score, color_index) VALUES " +
                    "(10, 1, 0, 'Omar', 40, 0), (11, 1, 1, 'Mona', 120, 1), (12, 1, 2, 'Ali', 40, 2), " +
                    "(20, 2, 0, 'omar', 0, 0), (21, 2, 1, 'Sara', 0, 1), " +
                    "(30, 3, 0, 'MONA', 10, 0), (31, 3, 1, 'Ali', 0, 1)",
            )
            execSQL(
                "INSERT INTO rounds (id, game_id, number, status, started_at) " +
                    "VALUES (40, 3, 1, 'IN_PROGRESS', ${startedAt + 3})",
            )
            close()
        }

        // Checks the migrated schema against the exported 3.json.
        helper.runMigrationsAndValidate(3, DominoDatabase.MIGRATIONS.toList()).close()

        val database = openWithRoom()
        try {
            val dao = database.gameDao()
            assertEquals(listOf("Omar", "Mona", "Ali", "Sara"), dao.findProfiles().map { it.name })
            val profileOf = (10L..31L).mapNotNull { id -> dao.findPlayer(id)?.let { id to it.profileId } }.toMap()
            assertEquals(profileOf.getValue(10), profileOf.getValue(20))
            assertEquals(profileOf.getValue(11), profileOf.getValue(30))
            assertEquals(profileOf.getValue(12), profileOf.getValue(31))
            assertNotNull(profileOf.getValue(21))

            val standings = dao.findFinalStandings(1)
            assertEquals(listOf(11L, 10L, 12L), standings.map { it.playerId })
            assertEquals(listOf(1, 2, 2), standings.map { it.rank })
            assertEquals(listOf(true, false, false), standings.map { it.isKing })
            assertEquals(listOf(false, true, true), standings.map { it.isKooz })
            assertEquals(emptyList<Any>(), dao.findFinalStandings(2) + dao.findFinalStandings(3))

            // The game in progress goes on, and a new game reuses the migrated profiles.
            val games = RoomGameRepository(database)
            assertEquals(listOf(10, 0), games.findActiveGame()!!.players.map { it.score })
            games.startGame(
                NewGame(
                    target = TargetScore.of(100).getOrNull()!!,
                    playerNames = listOf("sara", "Hana").map { PlayerName.of(it).getOrNull()!! },
                    startedAt = Instant.ofEpochMilli(startedAt + 4),
                ),
            )
            val newGame = games.findActiveGame()!!
            assertEquals(profileOf.getValue(21), dao.findPlayer(newGame.players[0].id.value)!!.profileId)
            assertEquals(5, dao.findProfiles().size)
        } finally {
            database.close()
        }
    }

    /** Opens the file the way the app does (no driver), so the migration also runs on that path. */
    private fun openWithRoom(): DominoDatabase = Room.databaseBuilder(context, DominoDatabase::class.java, NAME)
        .addMigrations(*DominoDatabase.MIGRATIONS)
        .allowMainThreadQueries()
        .build()

    private companion object {
        const val NAME = "migration-test.db"
    }
}
