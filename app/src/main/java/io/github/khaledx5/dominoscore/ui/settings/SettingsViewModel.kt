package io.github.khaledx5.dominoscore.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.audio.MusicSettings
import io.github.khaledx5.dominoscore.audio.MusicSettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languageSettings: LanguageSettings,
    private val musicSettingsStore: MusicSettingsStore,
) : ViewModel() {

    private val _language = MutableStateFlow(languageSettings.current())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    val music: StateFlow<MusicSettings> = musicSettingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MusicSettings())

    fun selectLanguage(language: AppLanguage) {
        if (language == _language.value) return
        _language.value = language
        languageSettings.change(language)
    }

    fun setGameplayMusicEnabled(enabled: Boolean) {
        viewModelScope.launch { musicSettingsStore.setGameplayEnabled(enabled) }
    }

    fun setWinnerMusicEnabled(enabled: Boolean) {
        viewModelScope.launch { musicSettingsStore.setWinnerEnabled(enabled) }
    }

    fun setMusicVolume(volume: Float) {
        viewModelScope.launch { musicSettingsStore.setVolume(volume) }
    }

    fun setEventSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch { musicSettingsStore.setEventSoundsEnabled(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
