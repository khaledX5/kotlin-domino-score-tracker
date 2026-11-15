package io.github.khaledx5.dominoscore.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

enum class GameEventType {
    TARGET_CHANGED,
    ROUND_COMPLETED,
    WINNER_CONFIRMED,
    GAME_COMPLETED,
    PLAYER_RENAMED,
}

/** What happened in a game besides score entries, kept for the game detail screen. */
@Entity(
    tableName = "game_events",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["round_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("game_id"), Index("round_id"), Index("player_id")],
)
data class GameEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    @ColumnInfo(name = "round_id") val roundId: Long? = null,
    val type: GameEventType,
    @ColumnInfo(name = "old_value") val oldValue: String? = null,
    @ColumnInfo(name = "new_value") val newValue: String? = null,
    @ColumnInfo(name = "player_id") val playerId: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
