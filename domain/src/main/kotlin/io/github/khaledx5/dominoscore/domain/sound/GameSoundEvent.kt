package io.github.khaledx5.dominoscore.domain.sound

/**
 * Moments at the table worth a sound, highest priority first: when one command causes several,
 * only the first in this order plays.
 */
enum class GameSoundEvent {
    /** The number of qualified players went from fewer than two to two or more. */
    SECOND_QUALIFIED,

    /** The top two players both entered the last stretch before the target. */
    FINAL_STRETCH,

    /** The only player still on 0 finally scored. */
    LAST_PLAYER_SCORES,

    /** Two or more players landed on the same score, high enough to matter. */
    EQUAL_SCORES,

    /** One penalty bigger than the usual small ones. */
    LARGE_PENALTY,
}

/** The thresholds of the sound events, in one place so they are easy to tune. */
data class GameSoundRules(
    /** A penalty above this amount is large. */
    val largePenaltyAbove: Int = 4,
    /** Equal scores only count above this score. */
    val equalScoresAbove: Int = 25,
    /** The final stretch is the last this many points before the target. */
    val finalStretchPoints: Int = 10,
)
