package io.github.khaledx5.dominoscore.data.local

import androidx.room.Embedded
import androidx.room.Relation
import io.github.khaledx5.dominoscore.data.local.entity.GameEntity
import io.github.khaledx5.dominoscore.data.local.entity.PlayerEntity
import io.github.khaledx5.dominoscore.data.local.entity.RoundEntity

/** A game row loaded together with its players and rounds in one read transaction. */
data class GameWithDetails(
    @Embedded val game: GameEntity,
    @Relation(parentColumn = "id", entityColumn = "game_id")
    val players: List<PlayerEntity>,
    @Relation(parentColumn = "id", entityColumn = "game_id")
    val rounds: List<RoundEntity>,
)
