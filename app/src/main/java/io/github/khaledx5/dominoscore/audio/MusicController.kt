package io.github.khaledx5.dominoscore.audio

import io.github.khaledx5.dominoscore.di.ApplicationScope
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The music rules of the app, on top of [GameAudioManager]: settings are applied as soon as they
 * change, gameplay music only plays when enabled and asked for, and the winner tune only plays
 * when enabled. Screens talk to this class, never to the players.
 */
@Singleton
class MusicController @Inject constructor(
    private val audio: GameAudioManager,
    settingsStore: MusicSettingsStore,
    observeActiveGame: ObserveActiveGameUseCase,
    @param:ApplicationScope scope: CoroutineScope,
) {
    val settings: StateFlow<MusicSettings> = settingsStore.settings
        .stateIn(scope, SharingStarted.Eagerly, MusicSettings())

    val gameplayState: StateFlow<GameplayMusicState> = audio.gameplayState

    init {
        scope.launch {
            settings.collect { current ->
                audio.setVolume(current.volume)
                if (!current.gameplayEnabled) audio.stopGameplayMusic()
            }
        }
        // The game ended or another one replaced it: its music stops with it.
        scope.launch {
            observeActiveGame()
                .map { it?.game?.id }
                .distinctUntilChanged()
                .drop(1)
                .collect { audio.stopGameplayMusic() }
        }
    }

    /** The music button: off -> play, playing -> pause, paused -> resume from the same spot. */
    fun toggleGameplayMusic() {
        if (!settings.value.gameplayEnabled) return
        when (audio.gameplayState.value) {
            GameplayMusicState.STOPPED -> audio.playGameplayMusic()
            GameplayMusicState.PLAYING -> audio.pauseGameplayMusic()
            GameplayMusicState.PAUSED -> audio.resumeGameplayMusic()
        }
    }

    /** The app left the screen: music must not keep playing in the background. */
    fun onAppBackground() = audio.pauseGameplayMusic()

    fun stopGameplayMusic() = audio.stopGameplayMusic()

    /** Called only after the table confirmed the winner. */
    fun celebrateWinner() {
        audio.stopGameplayMusic()
        if (settings.value.winnerEnabled) audio.playWinnerMusic()
    }
}
