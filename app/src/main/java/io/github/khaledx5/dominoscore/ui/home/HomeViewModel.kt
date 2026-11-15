package io.github.khaledx5.dominoscore.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(observeActiveGame: ObserveActiveGameUseCase) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = observeActiveGame()
        .map { snapshot -> HomeUiState.Ready(snapshot?.toActiveGameUi()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState.Loading)

    private companion object {
        /** Keeps the database query alive through a configuration change. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
