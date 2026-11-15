package io.github.khaledx5.dominoscore.data.local

import androidx.room.TypeConverter
import java.time.Instant

/** Stores instants as epoch milliseconds, which sort correctly in SQL. */
class InstantConverters {
    @TypeConverter
    fun toEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun fromEpochMillis(millis: Long?): Instant? = millis?.let(Instant::ofEpochMilli)
}
