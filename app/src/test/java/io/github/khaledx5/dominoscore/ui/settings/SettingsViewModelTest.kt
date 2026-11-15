package io.github.khaledx5.dominoscore.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

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
        val viewModel = SettingsViewModel(RecordingLanguageSettings(AppLanguage.ARABIC))

        assertEquals(AppLanguage.ARABIC, viewModel.language.value)
    }

    @Test
    fun selectingALanguageAppliesIt() {
        val settings = RecordingLanguageSettings(AppLanguage.SYSTEM)
        val viewModel = SettingsViewModel(settings)

        viewModel.selectLanguage(AppLanguage.ARABIC)

        assertEquals(AppLanguage.ARABIC, viewModel.language.value)
        assertEquals(listOf(AppLanguage.ARABIC), settings.changes)
    }

    @Test
    fun selectingTheCurrentLanguageDoesNotRecreateTheScreen() {
        val settings = RecordingLanguageSettings(AppLanguage.ENGLISH)
        val viewModel = SettingsViewModel(settings)

        viewModel.selectLanguage(AppLanguage.ENGLISH)

        assertEquals(emptyList<AppLanguage>(), settings.changes)
    }
}
