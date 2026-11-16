package io.github.khaledx5.dominoscore.audio

import io.github.khaledx5.dominoscore.di.ApplicationScope
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent
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
 * change, the gameplay tune plays once per tap of the music button when enabled, and the winner
 * tune only plays when enabled. Screens talk to this class, never to the players.
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
                if (!current.eventSoundsEnabled) audio.stopEventSound()
            }
        }
        // The game ended or another one replaced it: its laugh and event sounds stop with it.
        scope.launch {
            observeActiveGame()
                .map { it?.game?.id }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    audio.stopGameplayMusic()
                    audio.stopEventSound()
                }
        }
    }

    /** The music button, like a doorbell: every tap plays the tune once from the start. */
    fun playGameplayMusic() {
        if (settings.value.gameplayEnabled) audio.playGameplayMusic()
    }

    /** The app left the screen: music must not keep playing in the background. */
    fun onAppBackground() = audio.stopAllMusic()

    fun stopGameplayMusic() = audio.stopGameplayMusic()

    /** A moment of the game happened: its clip plays once if event sounds are on and the winner tune is not playing. */
    fun playGameEvent(event: GameSoundEvent) {
        if (settings.value.eventSoundsEnabled) audio.playEventSound(event.track)
    }

    /** Called only after the table confirmed the winner. */
    fun celebrateWinner() {
        audio.stopGameplayMusic()
        if (settings.value.winnerEnabled) audio.playWinnerMusic()
    }
}
