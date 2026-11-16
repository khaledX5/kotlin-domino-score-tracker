package io.github.khaledx5.dominoscore.audio

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * What the player chose for sound. Gameplay music still only starts when the music button is tapped.
 * One switch turns every game event sound on or off; it never touches the laugh or the winner tune.
 */
data class MusicSettings(
    val gameplayEnabled: Boolean = true,
    val winnerEnabled: Boolean = true,
    val volume: Float = DEFAULT_VOLUME,
    val eventSoundsEnabled: Boolean = true,
) {

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

    private companion object {
        val GAMEPLAY_ENABLED = booleanPreferencesKey("music_gameplay_enabled")
        val WINNER_ENABLED = booleanPreferencesKey("music_winner_enabled")
        val VOLUME = floatPreferencesKey("music_volume")
        val EVENT_SOUNDS_ENABLED = booleanPreferencesKey("sound_events_enabled")
    }
}
