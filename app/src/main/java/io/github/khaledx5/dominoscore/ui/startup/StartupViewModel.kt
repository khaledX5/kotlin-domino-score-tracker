package io.github.khaledx5.dominoscore.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Runs the loading screen once per launch. The real work is the database's first read (it opens
 * the file and runs any migration), and it starts at once, in parallel with the entrance. The screen
 * stays until both the read and the entrance ([INTRO_MS]) are done, says ready for [FINISH_MS], then
 * fades into home. A slow read keeps it up as long as it takes. Activity scoped, so a rotation or
 * coming back to home never plays it again.
 */
@HiltViewModel
class StartupViewModel @Inject constructor(observeActiveGame: ObserveActiveGameUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(StartupUiState.Loading)
    val uiState: StateFlow<StartupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            coroutineScope {
                launch { delay(INTRO_MS) }
                observeActiveGame().first()
            }
            _uiState.value = StartupUiState.Finishing
            delay(FINISH_MS)
            _uiState.value = StartupUiState.Ready
        }
    }

    companion object {
        /** Long enough for the icon, the title and the three dominoes to settle. */
        const val INTRO_MS = 1_600L

        /** How long the full bar and "Ready!" stay before home fades in. */
        const val FINISH_MS = 400L
    }
}
