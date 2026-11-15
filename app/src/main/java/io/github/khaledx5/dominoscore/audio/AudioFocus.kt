package io.github.khaledx5.dominoscore.audio

import android.media.AudioFocusRequest
import android.media.AudioManager
import javax.inject.Inject

enum class FocusChange {
    GAINED,

    /** Another app took over for good (music app, video): stop until the user starts again. */
    LOST,

    /** A short interruption such as a call or a navigation prompt: pause and resume after it. */
    LOST_TRANSIENTLY,

    /** Something short plays over us: keep going, quieter. */
    DUCK,
}

/** Audio focus, behind an interface so the playback rules can be tested without Android. */
interface AudioFocus {
    fun request(onChange: (FocusChange) -> Unit): Boolean

    fun abandon()
}

class SystemAudioFocus @Inject constructor(private val audioManager: AudioManager) : AudioFocus {

    private var activeRequest: AudioFocusRequest? = null

    override fun request(onChange: (FocusChange) -> Unit): Boolean {
        val request = activeRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(GameMusicAttributes)
            .setOnAudioFocusChangeListener { change -> change.toFocusChange()?.let(onChange) }
            .build()
            .also { activeRequest = it }
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun abandon() {
        activeRequest?.let(audioManager::abandonAudioFocusRequest)
        activeRequest = null
    }
}

private fun Int.toFocusChange(): FocusChange? = when (this) {
    AudioManager.AUDIOFOCUS_GAIN -> FocusChange.GAINED
    AudioManager.AUDIOFOCUS_LOSS -> FocusChange.LOST
    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> FocusChange.LOST_TRANSIENTLY
    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> FocusChange.DUCK
    else -> null
}
