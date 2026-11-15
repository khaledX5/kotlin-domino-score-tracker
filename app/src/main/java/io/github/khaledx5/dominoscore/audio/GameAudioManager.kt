package io.github.khaledx5.dominoscore.audio

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GameplayMusicState {
    STOPPED,
    PLAYING,
    PAUSED,
}

/**
 * Plays the app's music. One gameplay player and one winner player at most, never both at once.
 * Gameplay music keeps its position while paused; stopping releases it. Missing or unreadable
 * files simply mean no music. Call from the main thread.
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
    private var resumeWhenFocusReturns = false

    fun playGameplayMusic() {
        if (_gameplayState.value == GameplayMusicState.PLAYING) return
        stopWinnerMusic()
        val player = gameplayPlayer
            ?: playerFactory.create(MusicTrack.GAMEPLAY, onCompletion = {})?.also { gameplayPlayer = it }
            ?: return
        if (!audioFocus.request(::onFocusChange)) return
        player.setVolume(volume)
        player.start()
        resumeWhenFocusReturns = false
        _gameplayState.value = GameplayMusicState.PLAYING
    }

    fun pauseGameplayMusic() {
        if (_gameplayState.value != GameplayMusicState.PLAYING) return
        gameplayPlayer?.pause()
        resumeWhenFocusReturns = false
        _gameplayState.value = GameplayMusicState.PAUSED
        abandonFocusIfSilent()
    }

    /** Continues from where the music was paused. Does nothing unless it is paused. */
    fun resumeGameplayMusic() {
        if (_gameplayState.value == GameplayMusicState.PAUSED) playGameplayMusic()
    }

    fun stopGameplayMusic() {
        gameplayPlayer?.release()
        gameplayPlayer = null
        resumeWhenFocusReturns = false
        _gameplayState.value = GameplayMusicState.STOPPED
        abandonFocusIfSilent()
    }

    /** Stops the gameplay music first, so the two tracks never overlap. */
    fun playWinnerMusic() {
        stopGameplayMusic()
        stopWinnerMusic()
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
        if (_gameplayState.value != GameplayMusicState.PLAYING && winnerPlayer == null) audioFocus.abandon()
    }

    private fun applyVolume(value: Float) {
        gameplayPlayer?.setVolume(value)
        winnerPlayer?.setVolume(value)
    }

    private fun onFocusChange(change: FocusChange) {
        when (change) {
            FocusChange.LOST -> {
                pauseGameplayMusic()
                stopWinnerMusic()
            }

            FocusChange.LOST_TRANSIENTLY -> if (_gameplayState.value == GameplayMusicState.PLAYING) {
                // Keep the focus request: the system gives it back after the interruption.
                gameplayPlayer?.pause()
                resumeWhenFocusReturns = true
                _gameplayState.value = GameplayMusicState.PAUSED
            }

            FocusChange.DUCK -> applyVolume(volume * DUCK_FACTOR)

            FocusChange.GAINED -> {
                applyVolume(volume)
                if (resumeWhenFocusReturns) {
                    resumeWhenFocusReturns = false
                    gameplayPlayer?.start()
                    _gameplayState.value = GameplayMusicState.PLAYING
                }
            }
        }
    }

    private companion object {
        const val SILENT = 0f
        const val FULL_VOLUME = 1f
        const val DUCK_FACTOR = 0.2f
    }
}
