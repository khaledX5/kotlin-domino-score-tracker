package io.github.khaledx5.dominoscore.testing

import io.github.khaledx5.dominoscore.audio.AudioFocus
import io.github.khaledx5.dominoscore.audio.FocusChange
import io.github.khaledx5.dominoscore.audio.GameAudioManager
import io.github.khaledx5.dominoscore.audio.MusicPlayer
import io.github.khaledx5.dominoscore.audio.MusicTrack

/** A real [GameAudioManager] on fake players, recording what played, for ViewModel and controller tests. */
class RecordingAudio {
    val started = mutableListOf<MusicTrack>()
    var volume = -1f
        private set

    val manager = GameAudioManager(
        playerFactory = { track, _ ->
            object : MusicPlayer {
                override fun start() {
                    started += track
                }

                override fun pause() = Unit

                override fun setVolume(volume: Float) {
                    this@RecordingAudio.volume = volume
                }

                override fun release() = Unit
            }
        },
        audioFocus = object : AudioFocus {
            override fun request(onChange: (FocusChange) -> Unit) = true

            override fun abandon() = Unit
        },
    )
}
