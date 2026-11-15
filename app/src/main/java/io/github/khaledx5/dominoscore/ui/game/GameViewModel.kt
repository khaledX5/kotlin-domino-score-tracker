package io.github.khaledx5.dominoscore.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.audio.GameplayMusicState
import io.github.khaledx5.dominoscore.audio.MusicController
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class GameViewModel @Inject constructor(
    observeActiveGame: ObserveActiveGameUseCase,
    private val music: MusicController,
) : ViewModel() {

    val uiState: StateFlow<GameUiState> = combine(
        observeActiveGame(),
        music.gameplayState,
        music.settings,
    ) { snapshot, musicState, musicSettings ->
        if (snapshot == null) {
            GameUiState.NoActiveGame
        } else {
            GameUiState.Playing(
                game = snapshot.toGameUi(),
                music = MusicButtonUi(
                    visible = musicSettings.gameplayEnabled,
                    playing = musicState == GameplayMusicState.PLAYING,
                ),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GameUiState.Loading)

    fun toggleMusic() = music.toggleGameplayMusic()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
