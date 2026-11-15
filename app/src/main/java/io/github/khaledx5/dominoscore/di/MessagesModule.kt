package io.github.khaledx5.dominoscore.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.khaledx5.dominoscore.domain.messages.MessageLibrarySource
import io.github.khaledx5.dominoscore.messages.AssetMessageLibrarySource
import javax.inject.Singleton

/** The ranking messages: the approved library in assets, read once per process. */
@Module
@InstallIn(SingletonComponent::class)
object MessagesModule {

    @Provides
    @Singleton
    fun provideMessageLibrarySource(@ApplicationContext context: Context): MessageLibrarySource =
        AssetMessageLibrarySource(context)
}
