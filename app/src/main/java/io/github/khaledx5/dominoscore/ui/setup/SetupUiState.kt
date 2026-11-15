package io.github.khaledx5.dominoscore.ui.setup

import androidx.compose.runtime.Immutable
import io.github.khaledx5.dominoscore.domain.model.Game

sealed interface TargetChoice {
    data class Preset(val value: Int) : TargetChoice

    data object Custom : TargetChoice
}

enum class TargetFieldError {
    NOT_A_NUMBER,
    TOO_LOW,
    TOO_HIGH,
}

enum class NameFieldError {
    BLANK,
    TOO_LONG,
    DUPLICATE,
}

/**
 * Everything the setup screen shows. All four name fields are kept, so going from 4 players
 * to 2 and back does not lose what was typed.
 */
@Immutable
data class SetupUiState(
    val targetChoice: TargetChoice = TargetChoice.Preset(DEFAULT_TARGET),
    val customTarget: String = "",
    val targetError: TargetFieldError? = null,
    val playerCount: Int = Game.PLAYER_COUNT.first,
    val names: List<String> = List(Game.PLAYER_COUNT.last) { "" },
    val nameErrors: Map<Int, NameFieldError> = emptyMap(),
    val isStarting: Boolean = false,
    val confirmReplaceActiveGame: Boolean = false,
    val gameStarted: Boolean = false,
) {
    val visibleNames: List<String> get() = names.take(playerCount)

    companion object {
        val TARGET_PRESETS = listOf(50, 100, 150, 200)
        const val DEFAULT_TARGET = 100
    }
}
