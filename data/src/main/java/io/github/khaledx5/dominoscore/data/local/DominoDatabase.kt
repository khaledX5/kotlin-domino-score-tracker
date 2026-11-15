package io.github.khaledx5.dominoscore.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import io.github.khaledx5.dominoscore.data.local.entity.FinalStandingEntity
import io.github.khaledx5.dominoscore.data.local.entity.GameEntity
import io.github.khaledx5.dominoscore.data.local.entity.GameEventEntity
import io.github.khaledx5.dominoscore.data.local.entity.PlayerEntity
import io.github.khaledx5.dominoscore.data.local.entity.PlayerMessageEntity
import io.github.khaledx5.dominoscore.data.local.entity.ProfileEntity
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
        ProfileEntity::class,
        FinalStandingEntity::class,
    ],
    version = 3,
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

        /** Hand-written migrations, for changes that need data filled in. Add them to every builder. */
        val MIGRATIONS: Array<Migration> = arrayOf(Migration2To3)
    }
}
