package io.github.khaledx5.dominoscore.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * One person across games. The table types a name at setup, and the same name (without case) is
 * the same person, so [nameKey] is unique. [name] is how the person was first written.
 */
@Entity(
    tableName = "profiles",
    indices = [Index(value = ["name_key"], unique = true)],
)
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "name_key") val nameKey: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
