package io.github.khaledx5.dominoscore.domain.model

import java.util.Locale

enum class PlayerNameError {
    BLANK,
    TOO_LONG,
}

/** A trimmed display name of 1 to [MAX_LENGTH] characters. */
@JvmInline
value class PlayerName private constructor(val value: String) {

    /**
     * The name without case: "Ali" and "ali" share one key, so they count as the same player in a
     * game and as the same person across games.
     */
    val key: String get() = value.lowercase(Locale.ROOT)

    fun matches(other: PlayerName): Boolean = key == other.key

    companion object {
        const val MAX_LENGTH = 20

        fun of(raw: String): Outcome<PlayerName, PlayerNameError> {
            val trimmed = raw.trim()
            return when {
                trimmed.isEmpty() -> Outcome.Failure(PlayerNameError.BLANK)
                trimmed.length > MAX_LENGTH -> Outcome.Failure(PlayerNameError.TOO_LONG)
                else -> Outcome.Success(PlayerName(trimmed))
            }
        }
    }
}
