package io.github.khaledx5.dominoscore.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.khaledx5.dominoscore.data.local.entity.GameEntity
import io.github.khaledx5.dominoscore.data.local.entity.GameEventEntity
import io.github.khaledx5.dominoscore.data.local.entity.PlayerEntity
import io.github.khaledx5.dominoscore.data.local.entity.PlayerMessageEntity
import io.github.khaledx5.dominoscore.data.local.entity.RoundEntity
import io.github.khaledx5.dominoscore.data.local.entity.ScoreOperationEntity

@Database(
    entities = [
        GameEntity::class,
        PlayerEntity::class,
        RoundEntity::class,
        ScoreOperationEntity::class,
        GameEventEntity::class,
        PlayerMessageEntity::class,
    ],
    version = 2,
    exportSchema = true,
    // v2 only adds the player_messages table, so Room can generate the migration from the exported schemas.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(InstantConverters::class)
abstract class DominoDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    abstract fun playerMessageDao(): PlayerMessageDao

    companion object {
        const val NAME = "domino-score.db"
    }
}
