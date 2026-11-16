package io.github.khaledx5.dominoscore.audio

import io.github.khaledx5.dominoscore.domain.command.GameTransition
import io.github.khaledx5.dominoscore.domain.command.GameTransitionListener
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEventDetector
import javax.inject.Inject

/**
 * Turns game commands into event sounds: the detector names at most one event per command, the
 * highest priority one, and the music rules decide whether and how it plays. It only hears commands
 * through the command runner, so opening, scrolling or rotating the game never plays anything.
 */
class GameSoundEvents @Inject constructor(
    private val detector: GameSoundEventDetector,
    private val music: MusicController,
) : GameTransitionListener {

    override suspend fun onTransition(transition: GameTransition) {
        if (!transition.after.isActive) return
        val event = detector.detect(transition) ?: return
        music.playGameEvent(event)
    }
}

/** The clip of each event. A second qualified player shares the final stretch clip. */
internal val GameSoundEvent.track: MusicTrack
    get() = when (this) {
        GameSoundEvent.SECOND_QUALIFIED, GameSoundEvent.FINAL_STRETCH -> MusicTrack.EVENT_FINAL_STRETCH
        GameSoundEvent.LAST_PLAYER_SCORES -> MusicTrack.EVENT_LAST_PLAYER_SCORES
        GameSoundEvent.EQUAL_SCORES -> MusicTrack.EVENT_EQUAL_SCORES
        GameSoundEvent.LARGE_PENALTY -> MusicTrack.EVENT_LARGE_PENALTY
    }
