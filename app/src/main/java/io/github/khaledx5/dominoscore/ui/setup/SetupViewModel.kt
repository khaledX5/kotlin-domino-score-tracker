package io.github.khaledx5.dominoscore.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerNameError
import io.github.khaledx5.dominoscore.domain.model.RangeError
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.rules.RosterIssue
import io.github.khaledx5.dominoscore.domain.usecase.ActiveGameExists
import io.github.khaledx5.dominoscore.domain.usecase.InvalidRoster
import io.github.khaledx5.dominoscore.domain.usecase.InvalidTarget
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Holds the setup form. The rules themselves live in [StartGameUseCase]; this class only maps them to fields. */
@HiltViewModel
class SetupViewModel @Inject constructor(private val startGame: StartGameUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun selectPresetTarget(value: Int) = _uiState.update {
        it.copy(targetChoice = TargetChoice.Preset(value), targetError = null)
    }

    fun selectCustomTarget() = _uiState.update { it.copy(targetChoice = TargetChoice.Custom, targetError = null) }

    fun updateCustomTarget(text: String) = _uiState.update {
        it.copy(customTarget = text.filter(Char::isDigit).take(MAX_TARGET_DIGITS), targetError = null)
    }

    fun selectPlayerCount(count: Int) {
        require(count in Game.PLAYER_COUNT) { "Unsupported player count $count" }
        _uiState.update { it.copy(playerCount = count, nameErrors = emptyMap()) }
    }

    /** Editing a name clears its own error and every duplicate mark, since a duplicate involves two fields. */
    fun updateName(seat: Int, name: String) = _uiState.update {
        val names = it.names.toMutableList().apply { set(seat, name) }
        val nameErrors = it.nameErrors.filter { (errorSeat, error) ->
            errorSeat != seat &&
                error != NameFieldError.DUPLICATE
        }
        it.copy(names = names, nameErrors = nameErrors)
    }

    fun start() = submit(replaceActiveGame = false)

    fun confirmReplaceActiveGame() = submit(replaceActiveGame = true)

    fun dismissReplaceActiveGame() = _uiState.update { it.copy(confirmReplaceActiveGame = false) }

    fun onGameStartedHandled() = _uiState.update { it.copy(gameStarted = false) }

    private fun submit(replaceActiveGame: Boolean) {
        val state = _uiState.value
        if (state.isStarting) return
        val target = when (val choice = state.targetChoice) {
            is TargetChoice.Preset -> choice.value
            TargetChoice.Custom -> state.customTarget.toIntOrNull()
        }
        if (target == null) {
            _uiState.update { it.copy(targetError = TargetFieldError.NOT_A_NUMBER) }
            return
        }
        _uiState.update { it.copy(isStarting = true, confirmReplaceActiveGame = false) }
        viewModelScope.launch {
            val result = startGame(target, state.visibleNames, replaceActiveGame)
            _uiState.update { current ->
                val updated = current.copy(isStarting = false)
                when (result) {
                    is Outcome.Success -> updated.copy(gameStarted = true)

                    is Outcome.Failure -> when (val error = result.error) {
                        ActiveGameExists -> updated.copy(confirmReplaceActiveGame = true)
                        is InvalidTarget -> updated.copy(targetError = error.error.toFieldError())
                        is InvalidRoster -> updated.copy(nameErrors = error.issues.toFieldErrors())
                    }
                }
            }
        }
    }

    private companion object {
        val MAX_TARGET_DIGITS = TargetScore.ALLOWED.last.toString().length
    }
}

private fun RangeError.toFieldError(): TargetFieldError = when (this) {
    RangeError.TOO_LOW -> TargetFieldError.TOO_LOW
    RangeError.TOO_HIGH -> TargetFieldError.TOO_HIGH
}

private fun List<RosterIssue>.toFieldErrors(): Map<Int, NameFieldError> = buildMap {
    this@toFieldErrors.forEach { issue ->
        when (issue) {
            is RosterIssue.InvalidName -> put(issue.seat, issue.error.toFieldError())

            is RosterIssue.DuplicateName -> issue.seats.forEach { put(it, NameFieldError.DUPLICATE) }

            // The screen only offers 2 to 4 players, so a wrong count means a bug in this ViewModel.
            is RosterIssue.WrongPlayerCount -> error("Setup sent ${issue.count} players")
        }
    }
}

private fun PlayerNameError.toFieldError(): NameFieldError = when (this) {
    PlayerNameError.BLANK -> NameFieldError.BLANK
    PlayerNameError.TOO_LONG -> NameFieldError.TOO_LONG
}
