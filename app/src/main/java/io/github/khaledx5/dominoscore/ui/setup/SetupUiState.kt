package io.github.khaledx5.dominoscore.ui.setup

import androidx.compose.runtime.Immutable
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.PlayerColors
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.getOrNull

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
 * to 2 and back does not lose what was typed. [knownPlayers] are people from earlier games,
 * the most recent first.
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
    val knownPlayers: List<String> = emptyList(),
    /** One palette index per seat, for all four seats like [names]. The visible ones are all different. */
    val colors: List<Int> = PlayerColors.defaultFor(Game.PLAYER_COUNT.last),
    /** The seat whose color sheet is open. */
    val colorPickerSeat: Int? = null,
) {
    val visibleColors: List<Int> get() = colors.take(playerCount)

    /** The colors [seat] may switch to: its own, and every color no other visible seat has. */
    fun availableColors(seat: Int): List<Int> {
        val taken = visibleColors.filterIndexed { other, _ -> other != seat }.toSet()
        return PlayerColors.INDICES.filter { it !in taken }
    }

    val visibleNames: List<String> get() = names.take(playerCount)

    /** Known people not typed in yet (without case), offered as one-tap picks. */
    val suggestedPlayers: List<String>
        get() {
            val typed = visibleNames.mapNotNull { PlayerName.of(it).getOrNull()?.key }.toSet()
            return knownPlayers.filter { PlayerName.of(it).getOrNull()?.key !in typed }
        }

    /** The seat a pick goes to: the first empty name field, or null when every field is filled. */
    val firstEmptySeat: Int? get() = visibleNames.indexOfFirst { it.isBlank() }.takeIf { it >= 0 }

    companion object {
        val TARGET_PRESETS = listOf(50, 100, 150, 200)
        const val DEFAULT_TARGET = 100
    }
}
