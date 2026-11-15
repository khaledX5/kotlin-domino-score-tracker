package io.github.khaledx5.dominoscore.audio

import io.github.khaledx5.dominoscore.audio.GameplayMusicState.PLAYING
import io.github.khaledx5.dominoscore.audio.GameplayMusicState.STOPPED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameAudioManagerTest {

    private class FakePlayer(val track: MusicTrack, val onCompletion: () -> Unit) : MusicPlayer {
        var playing = false
        var released = false
        var lastVolume = -1f
        var starts = 0

        override fun start() {
            playing = true
            starts++
        }

        override fun setVolume(volume: Float) {
            lastVolume = volume
        }

        override fun release() {
            playing = false
            released = true
        }
    }

    private class FakeFocus : AudioFocus {
        var granted = true
        var held = false
        var listener: ((FocusChange) -> Unit)? = null

        override fun request(onChange: (FocusChange) -> Unit): Boolean {
            listener = onChange
            held = granted
            return granted
        }

        override fun abandon() {
            held = false
        }
    }

    private val created = mutableListOf<FakePlayer>()
    private var filesMissing = false
    private val focus = FakeFocus()
    private val audio = GameAudioManager(
        playerFactory = { track, onCompletion ->
            if (filesMissing) null else FakePlayer(track, onCompletion).also { created += it }
        },
        audioFocus = focus,
    )

    private fun gameplay() = created.last { it.track == MusicTrack.GAMEPLAY }

    private fun winner() = created.last { it.track == MusicTrack.WINNER }

    @Test
    fun gameplayMusicIsStoppedUntilAsked() {
        assertEquals(STOPPED, audio.gameplayState.value)
        assertTrue(created.isEmpty())
    }

    @Test
    fun theGameplayTunePlaysOnceAndReleasesItselfWhenItEnds() {
        audio.playGameplayMusic()
        val tune = gameplay()
        assertEquals(PLAYING, audio.gameplayState.value)

        tune.onCompletion()

        assertTrue(tune.released)
        assertEquals(STOPPED, audio.gameplayState.value)
        assertFalse(focus.held)
    }

    @Test
    fun tappingAgainStartsTheTuneOverLikeADoorbell() {
        audio.playGameplayMusic()
        val first = gameplay()

        audio.playGameplayMusic()

        assertTrue(first.released)
        assertEquals(2, created.size)
        assertTrue(gameplay().playing)
        assertEquals(PLAYING, audio.gameplayState.value)
    }

    @Test
    fun stoppingReleasesThePlayer() {
        audio.playGameplayMusic()
        val player = gameplay()

        audio.stopGameplayMusic()

        assertEquals(STOPPED, audio.gameplayState.value)
        assertTrue(player.released)
        assertFalse(focus.held)
    }

    @Test
    fun winnerMusicStopsTheGameplayMusicFirst() {
        audio.playGameplayMusic()
        val gameplay = gameplay()

        audio.playWinnerMusic()

        assertTrue(gameplay.released)
        assertEquals(STOPPED, audio.gameplayState.value)
        assertTrue(winner().playing)
    }

    @Test
    fun winnerMusicReleasesItselfWhenItEnds() {
        audio.playWinnerMusic()
        val winner = winner()

        winner.onCompletion()

        assertTrue(winner.released)
        assertFalse(focus.held)
    }

    @Test
    fun missingFilesMeanNoMusicAndNoCrash() {
        filesMissing = true

        audio.playGameplayMusic()
        audio.playWinnerMusic()

        assertEquals(STOPPED, audio.gameplayState.value)
        assertFalse(focus.held)
    }

    @Test
    fun deniedFocusMeansNoMusic() {
        focus.granted = false

        audio.playGameplayMusic()

        assertEquals(STOPPED, audio.gameplayState.value)
        assertTrue(gameplay().released)
    }

    @Test
    fun aCallEndsTheTune() {
        audio.playGameplayMusic()
        val tune = gameplay()

        focus.listener!!(FocusChange.LOST_TRANSIENTLY)

        assertTrue(tune.released)
        assertEquals(STOPPED, audio.gameplayState.value)
    }

    @Test
    fun anotherAppTakingOverEndsTheWinnerTuneToo() {
        audio.playWinnerMusic()
        val tune = winner()

        focus.listener!!(FocusChange.LOST)

        assertTrue(tune.released)
        assertFalse(focus.held)
    }

    @Test
    fun duckingLowersTheVolumeUntilFocusReturns() {
        audio.setVolume(0.5f)
        audio.playGameplayMusic()

        focus.listener!!(FocusChange.DUCK)
        assertEquals(0.1f, gameplay().lastVolume, 0.001f)

        focus.listener!!(FocusChange.GAINED)
        assertEquals(0.5f, gameplay().lastVolume, 0.001f)
    }

    @Test
    fun volumeIsClampedAndAppliedToThePlayingTrack() {
        audio.playGameplayMusic()

        audio.setVolume(3f)

        assertEquals(1f, gameplay().lastVolume, 0.001f)
    }

    @Test
    fun releaseStopsEverything() {
        audio.playWinnerMusic()

        audio.release()

        assertTrue(winner().released)
        assertEquals(STOPPED, audio.gameplayState.value)
    }
}
