package io.github.khaledx5.dominoscore.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A seat in one game. The id never changes, so a rename keeps the whole score log attached. */
@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["game_id", "seat"], unique = true)],
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    val seat: Int,
    val name: String,
    val score: Int,
    @ColumnInfo(name = "color_index") val colorIndex: Int,
)
