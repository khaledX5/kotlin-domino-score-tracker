package io.github.khaledx5.dominoscore.domain.model

enum class PlayerNameError {
    BLANK,
    TOO_LONG,
}

/** A trimmed display name of 1 to [MAX_LENGTH] characters. */
@JvmInline
value class PlayerName private constructor(val value: String) {

    /** Names are compared without case, so "Ali" and "ali" count as the same player. */
    fun matches(other: PlayerName): Boolean = value.equals(other.value, ignoreCase = true)

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
