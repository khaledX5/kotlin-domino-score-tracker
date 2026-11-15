package io.github.khaledx5.dominoscore.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.khaledx5.dominoscore.domain.model.RoundStatus
import java.time.Instant

/** A round of one game. A COMPLETED row is never updated again. */
@Entity(
    tableName = "rounds",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["game_id", "number"], unique = true)],
)
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    val number: Int,
    val status: RoundStatus,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "completed_at") val completedAt: Instant? = null,
)
