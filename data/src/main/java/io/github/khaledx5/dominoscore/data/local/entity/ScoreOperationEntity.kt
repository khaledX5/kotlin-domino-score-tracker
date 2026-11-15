package io.github.khaledx5.dominoscore.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import java.time.Instant

/** Append-only score log. Rows are inserted, never updated or deleted on their own. */
@Entity(
    tableName = "score_operations",
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
data class ScoreOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    @ColumnInfo(name = "round_id") val roundId: Long,
    @ColumnInfo(name = "player_id") val playerId: Long,
    val type: ScoreOperationType,
    val amount: Int,
    @ColumnInfo(name = "previous_score") val previousScore: Int,
    @ColumnInfo(name = "new_score") val newScore: Int,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
