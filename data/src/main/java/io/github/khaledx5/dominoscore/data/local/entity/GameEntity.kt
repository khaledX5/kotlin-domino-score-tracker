package io.github.khaledx5.dominoscore.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import java.time.Instant

/**
 * One game. At most one row is ACTIVE; the repository checks that inside the transaction that
 * creates a game. [winnerPlayerId] has no foreign key because players already reference the game,
 * and a cycle of foreign keys would make inserts depend on order.
 */
@Entity(
    tableName = "games",
    indices = [Index("status")],
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "target_score") val targetScore: Int,
    val status: GameStatus,
    @ColumnInfo(name = "winner_player_id") val winnerPlayerId: Long? = null,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "completed_at") val completedAt: Instant? = null,
)
