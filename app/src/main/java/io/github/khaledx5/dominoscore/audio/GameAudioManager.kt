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

/**
 * Plays the app's tunes. Both play once, like a doorbell: tapping again starts the tune over,
 * and a finished tune releases its player. One tune at a time, never both. Missing or unreadable
 * files simply mean no sound. Call from the main thread.
 */
@Singleton
class GameAudioManager @Inject constructor(
    private val playerFactory: MusicPlayerFactory,
    private val audioFocus: AudioFocus,
) {
    private val _gameplayState = MutableStateFlow(GameplayMusicState.STOPPED)
    val gameplayState: StateFlow<GameplayMusicState> = _gameplayState.asStateFlow()

    private var gameplayPlayer: MusicPlayer? = null
    private var winnerPlayer: MusicPlayer? = null
    private var volume = FULL_VOLUME

    /** Plays the gameplay tune once from the start, cutting off a tune that is still playing. */
    fun playGameplayMusic() {
        stopAllMusic()
        val player = playerFactory.create(MusicTrack.GAMEPLAY, onCompletion = ::stopGameplayMusic) ?: return
        if (!audioFocus.request(::onFocusChange)) {
            player.release()
            return
        }
        gameplayPlayer = player
        player.setVolume(volume)
        player.start()
        _gameplayState.value = GameplayMusicState.PLAYING
    }

    fun stopGameplayMusic() {
        gameplayPlayer?.release()
        gameplayPlayer = null
        _gameplayState.value = GameplayMusicState.STOPPED
        abandonFocusIfSilent()
    }

    /** Stops the gameplay tune first, so the two never overlap. */
    fun playWinnerMusic() {
        stopAllMusic()
        val player = playerFactory.create(MusicTrack.WINNER, onCompletion = ::stopWinnerMusic) ?: return
        if (!audioFocus.request(::onFocusChange)) {
            player.release()
            return
        }
        winnerPlayer = player
        player.setVolume(volume)
        player.start()
    }

    fun stopAllMusic() {
        stopGameplayMusic()
        stopWinnerMusic()
    }

    fun setVolume(newVolume: Float) {
        volume = newVolume.coerceIn(SILENT, FULL_VOLUME)
        applyVolume(volume)
    }

    fun release() = stopAllMusic()

    private fun stopWinnerMusic() {
        winnerPlayer?.release()
        winnerPlayer = null
        abandonFocusIfSilent()
    }

    private fun abandonFocusIfSilent() {
        if (gameplayPlayer == null && winnerPlayer == null) audioFocus.abandon()
    }

    private fun applyVolume(value: Float) {
        gameplayPlayer?.setVolume(value)
        winnerPlayer?.setVolume(value)
    }

    /** A short tune is not worth resuming: a call or another app simply ends it. */
    private fun onFocusChange(change: FocusChange) {
        when (change) {
            FocusChange.LOST, FocusChange.LOST_TRANSIENTLY -> stopAllMusic()
            FocusChange.DUCK -> applyVolume(volume * DUCK_FACTOR)
            FocusChange.GAINED -> applyVolume(volume)
        }
    }

    private companion object {
        const val SILENT = 0f
        const val FULL_VOLUME = 1f
        const val DUCK_FACTOR = 0.2f
    }
}
