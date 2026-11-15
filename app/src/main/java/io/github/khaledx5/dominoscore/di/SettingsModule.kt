package io.github.khaledx5.dominoscore.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.khaledx5.dominoscore.ui.settings.AppCompatLanguageSettings
import io.github.khaledx5.dominoscore.ui.settings.LanguageSettings

@Module
@InstallIn(SingletonComponent::class)
interface SettingsModule {
    @Binds
    fun bindLanguageSettings(implementation: AppCompatLanguageSettings): LanguageSettings
}
