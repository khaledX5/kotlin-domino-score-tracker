package io.github.khaledx5.dominoscore.data.local

import androidx.room.Embedded
import androidx.room.Relation
import io.github.khaledx5.dominoscore.data.local.entity.FinalStandingEntity
import io.github.khaledx5.dominoscore.data.local.entity.GameEntity
import io.github.khaledx5.dominoscore.data.local.entity.PlayerEntity
import io.github.khaledx5.dominoscore.data.local.entity.ProfileEntity
import io.github.khaledx5.dominoscore.data.local.entity.RoundEntity

/** A seat with its frozen result and the person behind it. Both are null only in a corrupt database. */
data class SeatWithResult(
    @Embedded val player: PlayerEntity,
    @Relation(parentColumn = "id", entityColumn = "player_id")
    val standing: FinalStandingEntity?,
    @Relation(parentColumn = "profile_id", entityColumn = "id")
    val profile: ProfileEntity?,
)

/** A completed game loaded with its seats, results and rounds in one read transaction. */
data class CompletedGameRow(
    @Embedded val game: GameEntity,
    @Relation(entity = PlayerEntity::class, parentColumn = "id", entityColumn = "game_id")
    val seats: List<SeatWithResult>,
    @Relation(parentColumn = "id", entityColumn = "game_id")
    val rounds: List<RoundEntity>,
)
