package io.github.khaledx5.dominoscore.testing

import io.github.khaledx5.dominoscore.audio.MusicSettings
import io.github.khaledx5.dominoscore.audio.MusicSettingsStore
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FakeMusicSettingsStore(initial: MusicSettings = MusicSettings()) : MusicSettingsStore {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<MusicSettings> = state

    override suspend fun setGameplayEnabled(enabled: Boolean) = state.update { it.copy(gameplayEnabled = enabled) }

    override suspend fun setWinnerEnabled(enabled: Boolean) = state.update { it.copy(winnerEnabled = enabled) }

    override suspend fun setVolume(volume: Float) = state.update { it.copy(volume = volume) }

    override suspend fun setEventSoundsEnabled(enabled: Boolean) = state.update {
        it.copy(eventSoundsEnabled = enabled)
    }

    override suspend fun setEventSoundEnabled(event: GameSoundEvent, enabled: Boolean) = state.update {
        it.copy(mutedEvents = if (enabled) it.mutedEvents - event else it.mutedEvents + event)
    }

    override suspend fun setVisualReactionsEnabled(enabled: Boolean) =
        state.update { it.copy(visualReactionsEnabled = enabled) }
}
