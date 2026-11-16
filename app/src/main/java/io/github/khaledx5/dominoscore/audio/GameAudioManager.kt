package io.github.khaledx5.dominoscore.audio

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GameplayMusicState {
    STOPPED,
    PLAYING,
}

/** Milliseconds since boot, behind an interface so the event cooldown can be tested on the JVM. */
fun interface ElapsedTime {
    fun millis(): Long
}

/**
 * Plays the app's sounds. Each plays once, like a doorbell, and a finished one releases its player.
 * Only one sound plays at a time, by priority: the winner tune, then game event sounds, then the
 * laugh. Missing or unreadable files simply mean no sound. Call from the main thread.
 */
@Singleton
class GameAudioManager @Inject constructor(
    private val playerFactory: MusicPlayerFactory,
    private val audioFocus: AudioFocus,
    private val elapsedTime: ElapsedTime,
) {
    private val _gameplayState = MutableStateFlow(GameplayMusicState.STOPPED)
    val gameplayState: StateFlow<GameplayMusicState> = _gameplayState.asStateFlow()

    private var gameplayPlayer: MusicPlayer? = null
    private var eventPlayer: MusicPlayer? = null
    private var winnerPlayer: MusicPlayer? = null
    private var lastEventStartedAt: Long? = null
    private var volume = FULL_VOLUME

    /** Plays the gameplay tune once from the start. It is a tap, so it cuts off whatever is playing. */
    fun playGameplayMusic() {
        stopAllMusic()
        val player = start(MusicTrack.GAMEPLAY, onCompletion = ::stopGameplayMusic) ?: return
        gameplayPlayer = player
        _gameplayState.value = GameplayMusicState.PLAYING
    }

    fun stopGameplayMusic() {
        gameplayPlayer?.release()
        gameplayPlayer = null
        _gameplayState.value = GameplayMusicState.STOPPED
        abandonFocusIfSilent()
    }

    /**
     * Plays a game event sound once. The winner tune is never interrupted, and an event within
     * [EVENT_COOLDOWN_MS] of the last one is dropped, so rapid taps never stack sounds. Otherwise it
     * replaces an older event sound or the laugh.
     */
    fun playEventSound(track: MusicTrack) {
        require(track.isEventSound) { "$track is not a game event sound" }
        if (winnerPlayer != null) return
        val now = elapsedTime.millis()
        if (lastEventStartedAt?.let { now - it < EVENT_COOLDOWN_MS } == true) return
        stopAllMusic()
        eventPlayer = start(track, onCompletion = ::stopEventSound) ?: return
        lastEventStartedAt = now
    }

    fun stopEventSound() {
        eventPlayer?.release()
        eventPlayer = null
        abandonFocusIfSilent()
    }

    /** Stops everything else first, so nothing ever overlaps the winner tune. */
    fun playWinnerMusic() {
        stopAllMusic()
        winnerPlayer = start(MusicTrack.WINNER, onCompletion = ::stopWinnerMusic)
    }

    fun stopAllMusic() {
        stopGameplayMusic()
        stopEventSound()
        stopWinnerMusic()
    }

    fun setVolume(newVolume: Float) {
        volume = newVolume.coerceIn(SILENT, FULL_VOLUME)
        applyVolume(volume)
    }

    fun release() = stopAllMusic()

    /** Loads and starts [track], or returns null when the file is missing or focus is refused. */
    private fun start(track: MusicTrack, onCompletion: () -> Unit): MusicPlayer? {
        val player = playerFactory.create(track, onCompletion) ?: return null
        if (!audioFocus.request(::onFocusChange)) {
            player.release()
            return null
        }
        player.setVolume(volume)
        player.start()
        return player
    }

    private fun stopWinnerMusic() {
        winnerPlayer?.release()
        winnerPlayer = null
        abandonFocusIfSilent()
    }

    private fun abandonFocusIfSilent() {
        if (gameplayPlayer == null && eventPlayer == null && winnerPlayer == null) audioFocus.abandon()
    }

    private fun applyVolume(value: Float) {
        gameplayPlayer?.setVolume(value)
        eventPlayer?.setVolume(value)
        winnerPlayer?.setVolume(value)
    }

    /** A short sound is not worth resuming: a call or another app simply ends it. */
    private fun onFocusChange(change: FocusChange) {
        when (change) {
            FocusChange.LOST, FocusChange.LOST_TRANSIENTLY -> stopAllMusic()
            FocusChange.DUCK -> applyVolume(volume * DUCK_FACTOR)
            FocusChange.GAINED -> applyVolume(volume)
        }
    }

    companion object {
        /** A second event this soon after the last one is dropped. */
        const val EVENT_COOLDOWN_MS = 1_500L
        private const val SILENT = 0f
        private const val FULL_VOLUME = 1f
        private const val DUCK_FACTOR = 0.2f
    }
}
