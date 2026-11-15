package io.github.khaledx5.dominoscore.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.khaledx5.dominoscore.data.RoomGameRepository
import io.github.khaledx5.dominoscore.data.local.DominoDatabase
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import java.time.Clock
import javax.inject.Singleton

/** Wires the :data implementations to the :domain contracts. The rest of the app only sees the contracts. */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DominoDatabase =
        Room.databaseBuilder(context, DominoDatabase::class.java, DominoDatabase.NAME).build()

    @Provides
    @Singleton
    fun provideGameRepository(database: DominoDatabase): GameRepository = RoomGameRepository(database)

    /** Timestamps are stored in UTC; screens format them in the device time zone. */
    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}
