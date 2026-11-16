package io.github.khaledx5.dominoscore.audio

import io.github.khaledx5.dominoscore.domain.command.GameTransition
import io.github.khaledx5.dominoscore.domain.command.GameTransitionListener
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEventDetector
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Turns game commands into event sounds and visual reactions: the detector names at most one event
 * per command, the highest priority one, the music rules decide whether its sound plays, and the
 * game screen always shows it, sound on or off. It only hears commands through the command runner,
 * so opening, scrolling or rotating the game never fires one.
 */
@Singleton
class GameSoundEvents @Inject constructor(
    private val detector: GameSoundEventDetector,
    private val music: MusicController,
) : GameTransitionListener {

    private val _reactions = MutableSharedFlow<GameSoundEvent>(extraBufferCapacity = 1)

    /** Events to show on screen. Nobody watching means nothing is kept: an old reaction never shows up later. */
    val reactions: SharedFlow<GameSoundEvent> = _reactions.asSharedFlow()

    override suspend fun onTransition(transition: GameTransition) {
        if (!transition.after.isActive) return
        val event = detector.detect(transition) ?: return
        music.playGameEvent(event)
        _reactions.tryEmit(event)
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
