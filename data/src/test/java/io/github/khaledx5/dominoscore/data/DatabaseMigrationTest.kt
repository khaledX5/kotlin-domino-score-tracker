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
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A phone that already has v1 with a game in progress must keep it after updating the app. */
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

        val database = Room.databaseBuilder(context, DominoDatabase::class.java, NAME).allowMainThreadQueries().build()
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

    private companion object {
        const val NAME = "migration-test.db"
    }
}
