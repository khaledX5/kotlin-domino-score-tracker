package io.github.khaledx5.dominoscore.audio

import android.content.Context
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Music for a game: the system can route and duck it like any other game audio. */
internal val GameMusicAttributes: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_GAME)
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .build()

class MediaPlayerFactory @Inject constructor(@param:ApplicationContext private val context: Context) :
    MusicPlayerFactory {

    override fun create(track: MusicTrack, onCompletion: () -> Unit): MusicPlayer? {
        val mediaPlayer = try {
            MediaPlayer.create(context, track.resId, GameMusicAttributes, AudioManager.AUDIO_SESSION_ID_GENERATE)
        } catch (_: Resources.NotFoundException) {
            null
        } ?: return null
        mediaPlayer.isLooping = track.looping
        mediaPlayer.setOnCompletionListener { onCompletion() }
        return MediaPlayerMusicPlayer(mediaPlayer)
    }
}

private class MediaPlayerMusicPlayer(private val mediaPlayer: MediaPlayer) : MusicPlayer {
    override fun start() = mediaPlayer.start()

    override fun pause() = mediaPlayer.pause()

    override fun setVolume(volume: Float) = mediaPlayer.setVolume(volume, volume)

    override fun release() = mediaPlayer.release()
}
