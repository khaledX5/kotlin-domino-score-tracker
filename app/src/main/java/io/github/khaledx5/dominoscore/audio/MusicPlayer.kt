package io.github.khaledx5.dominoscore.audio

import androidx.annotation.RawRes
import io.github.khaledx5.dominoscore.R

/** The two sounds the app plays. Each plays once per request. GAMEPLAY is the laugh of the 😂 button. */
enum class MusicTrack(@param:RawRes val resId: Int) {
    GAMEPLAY(R.raw.laugh_sound),
    WINNER(R.raw.winner_music),
}

/** One loaded track. Wrapping the platform player keeps the playback rules testable on the JVM. */
interface MusicPlayer {
    fun start()

    fun setVolume(volume: Float)

    fun release()
}

fun interface MusicPlayerFactory {
    /** Returns null when the track cannot be loaded (missing or unreadable file): music is then silently skipped. */
    fun create(track: MusicTrack, onCompletion: () -> Unit): MusicPlayer?
}
