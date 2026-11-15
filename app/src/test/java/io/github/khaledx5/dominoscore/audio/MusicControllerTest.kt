package io.github.khaledx5.dominoscore.audio

import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.EndRoundUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.FakeMusicSettingsStore
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.RecordingAudio
import io.github.khaledx5.dominoscore.testing.testClock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MusicControllerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val audio = RecordingAudio()
    private val store = FakeMusicSettingsStore()
    private val games = FakeGameRepository()
    private val scope = TestScope(mainDispatcherRule.dispatcher)
    private val controller = MusicController(audio.manager, store, ObserveActiveGameUseCase(games), scope)
    private val startGame = StartGameUseCase(games, testClock)

    @Test
    fun theMusicButtonCyclesPlayPauseResume() {
        controller.toggleGameplayMusic()
        assertEquals(GameplayMusicState.PLAYING, controller.gameplayState.value)

        controller.toggleGameplayMusic()
        assertEquals(GameplayMusicState.PAUSED, controller.gameplayState.value)

        controller.toggleGameplayMusic()
        assertEquals(GameplayMusicState.PLAYING, controller.gameplayState.value)
    }

    @Test
    fun disabledGameplayMusicIgnoresTheButton() = runTest {
        store.setGameplayEnabled(false)

        controller.toggleGameplayMusic()

        assertEquals(GameplayMusicState.STOPPED, controller.gameplayState.value)
    }

    @Test
    fun disablingGameplayMusicStopsItAtOnce() = runTest {
        controller.toggleGameplayMusic()

        store.setGameplayEnabled(false)

        assertEquals(GameplayMusicState.STOPPED, controller.gameplayState.value)
    }

    @Test
    fun goingToTheBackgroundPausesTheMusic() {
        controller.toggleGameplayMusic()

        controller.onAppBackground()

        assertEquals(GameplayMusicState.PAUSED, controller.gameplayState.value)
    }

    @Test
    fun theWinnerTunePlaysOnlyWhenEnabled() = runTest {
        controller.celebrateWinner()
        assertEquals(listOf(MusicTrack.WINNER), audio.started)

        store.setWinnerEnabled(false)
        controller.celebrateWinner()
        assertEquals(listOf(MusicTrack.WINNER), audio.started)
    }

    @Test
    fun celebratingStopsTheGameplayMusic() {
        controller.toggleGameplayMusic()

        controller.celebrateWinner()

        assertEquals(GameplayMusicState.STOPPED, controller.gameplayState.value)
    }

    @Test
    fun theSavedVolumeIsApplied() = runTest {
        store.setVolume(0.3f)
        controller.toggleGameplayMusic()

        assertEquals(0.3f, audio.volume, 0.001f)
    }

    @Test
    fun replacingTheGameStopsItsMusic() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        controller.toggleGameplayMusic()

        startGame(targetScore = 100, playerNames = listOf("Ali", "Sara"), replaceActiveGame = true)

        assertEquals(GameplayMusicState.STOPPED, controller.gameplayState.value)
    }

    @Test
    fun theMusicKeepsPlayingWhileTheSameGameChanges() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        controller.toggleGameplayMusic()

        EndRoundUseCase(games, testClock)()

        assertEquals(GameplayMusicState.PLAYING, controller.gameplayState.value)
    }
}
