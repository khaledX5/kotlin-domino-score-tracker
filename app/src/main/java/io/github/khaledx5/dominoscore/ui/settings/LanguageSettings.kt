package io.github.khaledx5.dominoscore.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject

/** Reads and changes the app language. An interface, so the settings screen can be tested without Android. */
interface LanguageSettings {
    fun current(): AppLanguage

    fun change(language: AppLanguage)
}

/**
 * Uses the per-app language API: Android 13+ stores the choice in the system (and shows it in
 * the system settings); on older versions AppCompat stores it (autoStoreLocales in the manifest).
 * Changing it recreates the activity, so every screen reloads its strings and layout direction.
 */
class AppCompatLanguageSettings @Inject constructor() : LanguageSettings {

    override fun current(): AppLanguage =
        AppLanguage.fromLanguageTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())

    override fun change(language: AppLanguage) {
        val locales = language.languageTag?.let(LocaleListCompat::forLanguageTags)
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
