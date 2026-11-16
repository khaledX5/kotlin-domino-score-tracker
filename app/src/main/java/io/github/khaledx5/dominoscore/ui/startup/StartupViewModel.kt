package io.github.khaledx5.dominoscore.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Tells the activity when startup is really done. The only work the first screen waits for is the
 * database: its first read opens the file and runs any migration, so that read is the ready signal.
 * The home screen then reads the same game from an open database. Activity scoped, so a rotation or
 * moving between screens never starts it again.
 */
@HiltViewModel
class StartupViewModel @Inject constructor(observeActiveGame: ObserveActiveGameUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(StartupUiState.SystemSplash)
    val uiState: StateFlow<StartupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeActiveGame().first()
            _uiState.value = StartupUiState.Ready
        }
    }
}
