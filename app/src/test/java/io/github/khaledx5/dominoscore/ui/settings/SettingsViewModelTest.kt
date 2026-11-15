package io.github.khaledx5.dominoscore.ui.settings

import io.github.khaledx5.dominoscore.audio.MusicSettings
import io.github.khaledx5.dominoscore.testing.FakeMusicSettingsStore
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class RecordingLanguageSettings(var stored: AppLanguage) : LanguageSettings {
        val changes = mutableListOf<AppLanguage>()

        override fun current(): AppLanguage = stored

        override fun change(language: AppLanguage) {
            changes += language
            stored = language
        }
    }

    @Test
    fun startsWithTheStoredLanguage() {
        val viewModel = SettingsViewModel(RecordingLanguageSettings(AppLanguage.ARABIC), FakeMusicSettingsStore())

        assertEquals(AppLanguage.ARABIC, viewModel.language.value)
    }

    @Test
    fun selectingALanguageAppliesIt() {
        val settings = RecordingLanguageSettings(AppLanguage.SYSTEM)
        val viewModel = SettingsViewModel(settings, FakeMusicSettingsStore())

        viewModel.selectLanguage(AppLanguage.ARABIC)

        assertEquals(AppLanguage.ARABIC, viewModel.language.value)
        assertEquals(listOf(AppLanguage.ARABIC), settings.changes)
    }

    @Test
    fun selectingTheCurrentLanguageDoesNotRecreateTheScreen() {
        val settings = RecordingLanguageSettings(AppLanguage.ENGLISH)
        val viewModel = SettingsViewModel(settings, FakeMusicSettingsStore())

        viewModel.selectLanguage(AppLanguage.ENGLISH)

        assertEquals(emptyList<AppLanguage>(), settings.changes)
    }

    @Test
    fun musicChangesAreSaved() = runTest {
        val store = FakeMusicSettingsStore()
        val viewModel = SettingsViewModel(RecordingLanguageSettings(AppLanguage.SYSTEM), store)

        viewModel.setGameplayMusicEnabled(false)
        viewModel.setWinnerMusicEnabled(false)
        viewModel.setMusicVolume(0.4f)

        assertEquals(MusicSettings(gameplayEnabled = false, winnerEnabled = false, volume = 0.4f), store.settings.value)
    }
}
