package io.github.khaledx5.dominoscore.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.getOrNull
import io.github.khaledx5.dominoscore.domain.rules.rankPlayers

/**
 * v3 adds profiles (one person across games) and the frozen final standings of completed games.
 * Written by hand because existing rows need data: every player is linked to a profile by name
 * (the same rule as a new game: same name without case = same person), and every completed game
 * gets its final rank, KING and KOOZ from its final scores, which never change after completion.
 */
internal object Migration2To3 : Migration(2, 3) {

    override fun migrate(connection: SQLiteConnection) {
        createTables(connection)
        val seats = readSeats(connection)
        linkProfiles(connection, seats)
        freezeFinalStandings(connection, seats.filter { it.gameStatus == GameStatus.COMPLETED.name })
    }

    private fun createTables(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `name_key` TEXT NOT NULL, `created_at` INTEGER NOT NULL)",
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_profiles_name_key` ON `profiles` (`name_key`)")
        connection.execSQL(
            "ALTER TABLE `players` ADD COLUMN `profile_id` INTEGER " +
                "REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION",
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_players_profile_id` ON `players` (`profile_id`)")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `final_standings` (`player_id` INTEGER NOT NULL, " +
                "`rank` INTEGER NOT NULL, `is_king` INTEGER NOT NULL, `is_kooz` INTEGER NOT NULL, " +
                "PRIMARY KEY(`player_id`), FOREIGN KEY(`player_id`) REFERENCES `players`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
    }

    /** Every seat of every game, oldest game first, so a profile takes the name it was first written with. */
    private fun readSeats(connection: SQLiteConnection): List<StoredSeat> = connection.prepare(
        "SELECT players.id, players.game_id, players.seat, players.name, players.score, players.color_index, " +
            "games.status, games.started_at FROM players INNER JOIN games ON games.id = players.game_id " +
            "ORDER BY games.started_at, games.id, players.seat",
    ).use { statement ->
        buildList {
            while (statement.step()) {
                add(
                    StoredSeat(
                        playerId = statement.getLong(0),
                        gameId = statement.getLong(1),
                        seat = statement.getInt(2),
                        name = statement.getText(3),
                        score = statement.getInt(4),
                        colorIndex = statement.getInt(5),
                        gameStatus = statement.getText(6),
                        gameStartedAt = statement.getLong(7),
                    ),
                )
            }
        }
    }

    private fun linkProfiles(connection: SQLiteConnection, seats: List<StoredSeat>) {
        val profileIds = mutableMapOf<String, Long>()
        seats.forEach { seat ->
            val key = nameOf(seat).key
            val profileId = profileIds.getOrPut(key) { insertProfile(connection, seat, key) }
            connection.prepare("UPDATE players SET profile_id = ? WHERE id = ?").use { statement ->
                statement.bindLong(1, profileId)
                statement.bindLong(2, seat.playerId)
                statement.step()
            }
        }
    }

    private fun insertProfile(connection: SQLiteConnection, seat: StoredSeat, key: String): Long {
        connection.prepare("INSERT INTO profiles (name, name_key, created_at) VALUES (?, ?, ?)").use { statement ->
            statement.bindText(1, nameOf(seat).value)
            statement.bindText(2, key)
            statement.bindLong(3, seat.gameStartedAt)
            statement.step()
        }
        return connection.prepare("SELECT last_insert_rowid()").use { statement ->
            check(statement.step()) { "No row id after inserting a profile" }
            statement.getLong(0)
        }
    }

    private fun freezeFinalStandings(connection: SQLiteConnection, seats: List<StoredSeat>) {
        seats.groupBy { it.gameId }.values.forEach { game ->
            val players = game.map { seat ->
                Player(PlayerId(seat.playerId), seat.seat, nameOf(seat), seat.score, seat.colorIndex)
            }
            rankPlayers(players).forEach { standing ->
                connection.prepare(
                    "INSERT INTO final_standings (player_id, rank, is_king, is_kooz) VALUES (?, ?, ?, ?)",
                ).use { statement ->
                    statement.bindLong(1, standing.player.id.value)
                    statement.bindLong(2, standing.rank.toLong())
                    statement.bindBoolean(3, standing.isKing)
                    statement.bindBoolean(4, standing.isKooz)
                    statement.step()
                }
            }
        }
    }

    /** Names were validated before they were stored, so an invalid one means a corrupt database. */
    private fun nameOf(seat: StoredSeat): PlayerName =
        checkNotNull(PlayerName.of(seat.name).getOrNull()) { "Stored name of player ${seat.playerId} is invalid" }

    private class StoredSeat(
        val playerId: Long,
        val gameId: Long,
        val seat: Int,
        val name: String,
        val score: Int,
        val colorIndex: Int,
        val gameStatus: String,
        val gameStartedAt: Long,
    )
}
