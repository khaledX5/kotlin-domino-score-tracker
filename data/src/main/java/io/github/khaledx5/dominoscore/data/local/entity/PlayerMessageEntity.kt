package io.github.khaledx5.dominoscore.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory
import io.github.khaledx5.dominoscore.domain.messages.MessageTrigger
import java.time.Instant

/**
 * A ranking message a player got. Append-only, like the score log. Only the library id of the
 * line is stored: the text comes from the shipped library, so a fixed typo shows everywhere.
 */
@Entity(
    tableName = "player_messages",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["round_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("game_id"), Index("player_id"), Index("round_id")],
)
data class PlayerMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    @ColumnInfo(name = "player_id") val playerId: Long,
    @ColumnInfo(name = "round_id") val roundId: Long,
    @ColumnInfo(name = "message_id") val messageId: String,
    val category: MessageCategory,
    val trigger: MessageTrigger,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
