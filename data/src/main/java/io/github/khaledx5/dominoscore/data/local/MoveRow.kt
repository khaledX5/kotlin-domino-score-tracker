package io.github.khaledx5.dominoscore.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import io.github.khaledx5.dominoscore.data.local.entity.ScoreOperationEntity

/** A score operation with the number of its round, read in one query for the move history. */
data class MoveRow(
    @Embedded val operation: ScoreOperationEntity,
    @ColumnInfo(name = "round_number") val roundNumber: Int,
)
