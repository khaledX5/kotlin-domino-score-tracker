package io.github.khaledx5.dominoscore.audio

import androidx.annotation.RawRes
import io.github.khaledx5.dominoscore.R

/**
 * The sounds the app plays. Each plays once per request. GAMEPLAY is the laugh of the 😂 button;
 * the event sounds belong to moments of the game (the final stretch one also marks a second
 * qualified player).
 */
enum class MusicTrack(@param:RawRes val resId: Int, val isEventSound: Boolean = false) {
    GAMEPLAY(R.raw.laugh_sound),
    WINNER(R.raw.winner_music),
    EVENT_LARGE_PENALTY(R.raw.sfx_large_penalty, isEventSound = true),
    EVENT_EQUAL_SCORES(R.raw.sfx_equal_scores, isEventSound = true),
    EVENT_FINAL_STRETCH(R.raw.sfx_final_stretch, isEventSound = true),
    EVENT_LAST_PLAYER_SCORES(R.raw.sfx_last_player_scores, isEventSound = true),
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
