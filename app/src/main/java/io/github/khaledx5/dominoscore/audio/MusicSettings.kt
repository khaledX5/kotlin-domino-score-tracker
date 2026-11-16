package io.github.khaledx5.dominoscore.audio

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * What the player chose for sound. Gameplay music still only starts when the music button is tapped.
 * Game event sounds have a master switch over one switch per event; the visual reactions are
 * separate, so a silent phone can still show them. None of these touch the laugh or the winner tune.
 */
data class MusicSettings(
    val gameplayEnabled: Boolean = true,
    val winnerEnabled: Boolean = true,
    val volume: Float = DEFAULT_VOLUME,
    val eventSoundsEnabled: Boolean = true,
    val mutedEvents: Set<GameSoundEvent> = emptySet(),
    val visualReactionsEnabled: Boolean = true,
) {
    /** The event's sound plays only when both the master switch and its own switch are on. */
    fun playsSound(event: GameSoundEvent): Boolean = eventSoundsEnabled && event !in mutedEvents

    companion object {
        const val DEFAULT_VOLUME = 0.8f
    }
}

interface MusicSettingsStore {
    val settings: Flow<MusicSettings>

    suspend fun setGameplayEnabled(enabled: Boolean)

    suspend fun setWinnerEnabled(enabled: Boolean)

    suspend fun setVolume(volume: Float)

    suspend fun setEventSoundsEnabled(enabled: Boolean)

    suspend fun setEventSoundEnabled(event: GameSoundEvent, enabled: Boolean)

    suspend fun setVisualReactionsEnabled(enabled: Boolean)
}

/** Keeps the music settings on the device with DataStore, so they survive restarts and updates. */
class DataStoreMusicSettingsStore @Inject constructor(private val dataStore: DataStore<Preferences>) :
    MusicSettingsStore {

    override val settings: Flow<MusicSettings> = dataStore.data.map { preferences ->
        MusicSettings(
            gameplayEnabled = preferences[GAMEPLAY_ENABLED] ?: true,
            winnerEnabled = preferences[WINNER_ENABLED] ?: true,
            volume = preferences[VOLUME] ?: MusicSettings.DEFAULT_VOLUME,
            eventSoundsEnabled = preferences[EVENT_SOUNDS_ENABLED] ?: true,
            mutedEvents = GameSoundEvent.entries.filter { preferences[it.key] == false }.toSet(),
            visualReactionsEnabled = preferences[VISUAL_REACTIONS_ENABLED] ?: true,
        )
    }

    override suspend fun setGameplayEnabled(enabled: Boolean) {
        dataStore.edit { it[GAMEPLAY_ENABLED] = enabled }
    }

    override suspend fun setWinnerEnabled(enabled: Boolean) {
        dataStore.edit { it[WINNER_ENABLED] = enabled }
    }

    override suspend fun setVolume(volume: Float) {
        dataStore.edit { it[VOLUME] = volume.coerceIn(0f, 1f) }
    }

    override suspend fun setEventSoundsEnabled(enabled: Boolean) {
        dataStore.edit { it[EVENT_SOUNDS_ENABLED] = enabled }
    }

    override suspend fun setEventSoundEnabled(event: GameSoundEvent, enabled: Boolean) {
        dataStore.edit { it[event.key] = enabled }
    }

    override suspend fun setVisualReactionsEnabled(enabled: Boolean) {
        dataStore.edit { it[VISUAL_REACTIONS_ENABLED] = enabled }
    }

    private companion object {
        val GAMEPLAY_ENABLED = booleanPreferencesKey("music_gameplay_enabled")
        val WINNER_ENABLED = booleanPreferencesKey("music_winner_enabled")
        val VOLUME = floatPreferencesKey("music_volume")
        val EVENT_SOUNDS_ENABLED = booleanPreferencesKey("sound_events_enabled")
        val VISUAL_REACTIONS_ENABLED = booleanPreferencesKey("visual_reactions_enabled")

        /** One key per event, named after it, so a new event starts switched on. */
        val GameSoundEvent.key get() = booleanPreferencesKey("sound_event_${name.lowercase()}")
    }
}
