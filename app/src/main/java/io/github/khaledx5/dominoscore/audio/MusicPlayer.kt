package io.github.khaledx5.dominoscore.audio

import androidx.annotation.RawRes
import io.github.khaledx5.dominoscore.R

/** The two music tracks the app plays. Gameplay music loops; the winner tune plays once. */
enum class MusicTrack(@param:RawRes val resId: Int, val looping: Boolean) {
    GAMEPLAY(R.raw.gameplay_music, looping = true),
    WINNER(R.raw.winner_music, looping = false),
}

/** One loaded track. Wrapping the platform player keeps the playback rules testable on the JVM. */
interface MusicPlayer {
    fun start()

    fun pause()

    fun setVolume(volume: Float)

    fun release()
}

fun interface MusicPlayerFactory {
    /** Returns null when the track cannot be loaded (missing or unreadable file): music is then silently skipped. */
    fun create(track: MusicTrack, onCompletion: () -> Unit): MusicPlayer?
}
