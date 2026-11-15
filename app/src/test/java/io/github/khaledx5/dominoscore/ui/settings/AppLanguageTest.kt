package io.github.khaledx5.dominoscore.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun noStoredLocaleMeansTheSystemLanguage() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags(""))
    }

    @Test
    fun mapsStoredTagsIncludingRegions() {
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromLanguageTags("ar"))
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromLanguageTags("ar-EG"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTags("en-GB,ar"))
    }

    @Test
    fun anUnsupportedLanguageFallsBackToTheSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags("fr-FR"))
    }
}
