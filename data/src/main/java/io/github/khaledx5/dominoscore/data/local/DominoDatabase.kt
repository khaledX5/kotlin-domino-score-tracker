package io.github.khaledx5.dominoscore.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.khaledx5.dominoscore.data.local.entity.GameEntity
import io.github.khaledx5.dominoscore.data.local.entity.GameEventEntity
import io.github.khaledx5.dominoscore.data.local.entity.PlayerEntity
import io.github.khaledx5.dominoscore.data.local.entity.RoundEntity
import io.github.khaledx5.dominoscore.data.local.entity.ScoreOperationEntity

@Database(
    entities = [
        GameEntity::class,
        PlayerEntity::class,
        RoundEntity::class,
        ScoreOperationEntity::class,
        GameEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(InstantConverters::class)
abstract class DominoDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        const val NAME = "domino-score.db"
    }
}
