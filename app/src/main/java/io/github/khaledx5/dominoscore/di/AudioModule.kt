package io.github.khaledx5.dominoscore.di

import android.content.Context
import android.media.AudioManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.khaledx5.dominoscore.audio.AudioFocus
import io.github.khaledx5.dominoscore.audio.MediaPlayerFactory
import io.github.khaledx5.dominoscore.audio.MusicPlayerFactory
import io.github.khaledx5.dominoscore.audio.SystemAudioFocus

@Module
@InstallIn(SingletonComponent::class)
interface AudioModule {

    @Binds
    fun bindMusicPlayerFactory(implementation: MediaPlayerFactory): MusicPlayerFactory

    @Binds
    fun bindAudioFocus(implementation: SystemAudioFocus): AudioFocus

    companion object {
        @Provides
        fun provideAudioManager(@ApplicationContext context: Context): AudioManager =
            context.getSystemService(AudioManager::class.java)
    }
}
