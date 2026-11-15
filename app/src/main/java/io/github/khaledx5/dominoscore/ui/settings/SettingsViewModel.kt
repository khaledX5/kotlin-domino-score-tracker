package io.github.khaledx5.dominoscore.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(private val languageSettings: LanguageSettings) : ViewModel() {

    private val _language = MutableStateFlow(languageSettings.current())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun selectLanguage(language: AppLanguage) {
        if (language == _language.value) return
        _language.value = language
        languageSettings.change(language)
    }
}
