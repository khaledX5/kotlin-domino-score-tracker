package io.github.khaledx5.dominoscore.audio

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the player chose for music. Gameplay music still only starts when the music button is tapped. */
data class MusicSettings(
    val gameplayEnabled: Boolean = true,
    val winnerEnabled: Boolean = true,
    val volume: Float = DEFAULT_VOLUME,
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
}

/** Keeps the music settings on the device with DataStore, so they survive restarts and updates. */
class DataStoreMusicSettingsStore @Inject constructor(private val dataStore: DataStore<Preferences>) :
    MusicSettingsStore {

    override val settings: Flow<MusicSettings> = dataStore.data.map { preferences ->
        MusicSettings(
            gameplayEnabled = preferences[GAMEPLAY_ENABLED] ?: true,
            winnerEnabled = preferences[WINNER_ENABLED] ?: true,
            volume = preferences[VOLUME] ?: MusicSettings.DEFAULT_VOLUME,
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

    private companion object {
        val GAMEPLAY_ENABLED = booleanPreferencesKey("music_gameplay_enabled")
        val WINNER_ENABLED = booleanPreferencesKey("music_winner_enabled")
        val VOLUME = floatPreferencesKey("music_volume")
    }
}
