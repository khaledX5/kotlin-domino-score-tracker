package io.github.khaledx5.dominoscore.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.khaledx5.dominoscore.data.local.entity.PlayerMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerMessageDao {

    @Insert
    suspend fun insertAll(messages: List<PlayerMessageEntity>)

    @Query("SELECT * FROM player_messages WHERE game_id = :gameId ORDER BY id")
    suspend fun findForGame(gameId: Long): List<PlayerMessageEntity>

    /** Each player's newest row in the game. */
    @Query(
        "SELECT * FROM player_messages WHERE id IN " +
            "(SELECT MAX(id) FROM player_messages WHERE game_id = :gameId GROUP BY player_id)",
    )
    fun observeLatestForGame(gameId: Long): Flow<List<PlayerMessageEntity>>
}
