package io.github.khaledx5.dominoscore.di

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.khaledx5.dominoscore.audio.AudioFocus
import io.github.khaledx5.dominoscore.audio.DataStoreMusicSettingsStore
import io.github.khaledx5.dominoscore.audio.ElapsedTime
import io.github.khaledx5.dominoscore.audio.GameSoundEvents
import io.github.khaledx5.dominoscore.audio.MediaPlayerFactory
import io.github.khaledx5.dominoscore.audio.MusicPlayerFactory
import io.github.khaledx5.dominoscore.audio.MusicSettingsStore
import io.github.khaledx5.dominoscore.audio.SystemAudioFocus
import io.github.khaledx5.dominoscore.domain.command.GameTransitionListener
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEventDetector

@Module
@InstallIn(SingletonComponent::class)
interface AudioModule {

    @Binds
    fun bindMusicPlayerFactory(implementation: MediaPlayerFactory): MusicPlayerFactory

    @Binds
    fun bindAudioFocus(implementation: SystemAudioFocus): AudioFocus

    /** Game event sounds react to every game command through the command runner. */
    @Binds
    @IntoSet
    fun bindGameSoundEvents(implementation: GameSoundEvents): GameTransitionListener

    @Binds
    fun bindMusicSettingsStore(implementation: DataStoreMusicSettingsStore): MusicSettingsStore

    companion object {
        @Provides
        fun provideAudioManager(@ApplicationContext context: Context): AudioManager =
            context.getSystemService(AudioManager::class.java)

        /** Time since boot: unlike the wall clock it never jumps, so a cooldown stays exact. */
        @Provides
        fun provideGameSoundEventDetector(): GameSoundEventDetector = GameSoundEventDetector()

        @Provides
        fun provideElapsedTime(): ElapsedTime = ElapsedTime(SystemClock::elapsedRealtime)
    }
}
