package io.github.khaledx5.dominoscore.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.khaledx5.dominoscore.domain.command.GameTransitionListener
import io.github.khaledx5.dominoscore.domain.messages.MessageLibrarySource
import io.github.khaledx5.dominoscore.domain.messages.RankingMessageEngine
import io.github.khaledx5.dominoscore.messages.AssetMessageLibrarySource
import javax.inject.Singleton
import kotlin.random.Random

/** The ranking messages: the approved library in assets, read once per process. */
@Module
@InstallIn(SingletonComponent::class)
object MessagesModule {

    @Provides
    @Singleton
    fun provideMessageLibrarySource(@ApplicationContext context: Context): MessageLibrarySource =
        AssetMessageLibrarySource(context)

    /** Trash talk reacts to every game command through the command runner. */
    @Provides
    @IntoSet
    fun provideTrashTalkListener(engine: RankingMessageEngine): GameTransitionListener = engine

    /** Picks lines at random; tests pass a seeded Random instead. */
    @Provides
    fun provideRandom(): Random = Random.Default
}
