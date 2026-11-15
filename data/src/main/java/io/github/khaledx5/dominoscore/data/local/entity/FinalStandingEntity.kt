package io.github.khaledx5.dominoscore.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * A player's place when the game was completed, written once with the winner and never updated.
 * Only completed games have rows here.
 */
@Entity(
    tableName = "final_standings",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FinalStandingEntity(
    @PrimaryKey @ColumnInfo(name = "player_id") val playerId: Long,
    val rank: Int,
    @ColumnInfo(name = "is_king") val isKing: Boolean,
    @ColumnInfo(name = "is_kooz") val isKooz: Boolean,
)
