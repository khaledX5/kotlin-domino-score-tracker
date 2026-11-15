package io.github.khaledx5.dominoscore.audio

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** One write per test: on a Windows machine DataStore cannot replace its file a second time. */
class DataStoreMusicSettingsStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private val store = DataStoreMusicSettingsStore(
        PreferenceDataStoreFactory.create(scope = testScope.backgroundScope) {
            folder.root.resolve("settings.preferences_pb")
        },
    )

    @Test
    fun defaultsAreMusicOnAtEightyPercent() = testScope.runTest {
        assertEquals(MusicSettings(gameplayEnabled = true, winnerEnabled = true, volume = 0.8f), store.settings.first())
    }

    @Test
    fun savesTheGameplaySwitchAlone() = testScope.runTest {
        store.setGameplayEnabled(false)

        assertEquals(MusicSettings(gameplayEnabled = false), store.settings.first())
    }

    @Test
    fun savesTheWinnerSwitchAlone() = testScope.runTest {
        store.setWinnerEnabled(false)

        assertEquals(MusicSettings(winnerEnabled = false), store.settings.first())
    }

    @Test
    fun clampsTheVolume() = testScope.runTest {
        store.setVolume(4f)

        assertEquals(1f, store.settings.first().volume, 0.001f)
    }
}
