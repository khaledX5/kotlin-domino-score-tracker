package io.github.khaledx5.dominoscore.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.audio.GameplayMusicState
import io.github.khaledx5.dominoscore.audio.MusicController
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.RangeError
import io.github.khaledx5.dominoscore.domain.model.ScoreAmount
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.rules.PrimaryAction
import io.github.khaledx5.dominoscore.domain.usecase.ChangeTargetUseCase
import io.github.khaledx5.dominoscore.domain.usecase.EndRoundUseCase
import io.github.khaledx5.dominoscore.domain.usecase.InvalidAmount
import io.github.khaledx5.dominoscore.domain.usecase.InvalidTarget
import io.github.khaledx5.dominoscore.domain.usecase.NoActiveGame
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ScoreLimitReached
import io.github.khaledx5.dominoscore.domain.usecase.TargetUnchanged
import io.github.khaledx5.dominoscore.domain.usecase.UnknownPlayer
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GameViewModel @Inject constructor(
    observeActiveGame: ObserveActiveGameUseCase,
    private val recordScore: RecordScoreUseCase,
    private val changeTarget: ChangeTargetUseCase,
    private val endRound: EndRoundUseCase,
    private val music: MusicController,
) : ViewModel() {

    private val scoreEntry = MutableStateFlow<ScoreEntryUi?>(null)
    private val targetEdit = MutableStateFlow<TargetEditUi?>(null)
    private var endRoundJob: Job? = null

    val uiState: StateFlow<GameUiState> = combine(
        observeActiveGame(),
        music.gameplayState,
        music.settings,
        scoreEntry,
        targetEdit,
    ) { snapshot, musicState, musicSettings, entry, target ->
        if (snapshot == null) {
            GameUiState.NoActiveGame
        } else {
            GameUiState.Playing(
                game = snapshot.toGameUi(),
                music = MusicButtonUi(
                    visible = musicSettings.gameplayEnabled,
                    playing = musicState == GameplayMusicState.PLAYING,
                ),
                scoreEntry = entry,
                targetEdit = target,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GameUiState.Loading)

    fun toggleMusic() = music.toggleGameplayMusic()

    fun openScoreEntry(playerId: Long, playerName: String, type: ScoreOperationType) {
        scoreEntry.value = ScoreEntryUi(playerId = playerId, playerName = playerName, type = type)
    }

    fun dismissScoreEntry() {
        scoreEntry.value = null
    }

    fun updateAmount(text: String) = scoreEntry.update { entry ->
        entry?.copy(amountText = text.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS), error = null)
    }

    /** Saves the typed amount. */
    fun submitAmount() {
        val entry = scoreEntry.value ?: return
        val amount = entry.amountText.toIntOrNull()
        if (amount == null) {
            scoreEntry.value = entry.copy(error = ScoreEntryError.NOT_A_NUMBER)
            return
        }
        save(entry, amount)
    }

    /** Saves one of the quick amounts straight away. */
    fun submitQuickAmount(amount: Int) {
        scoreEntry.value?.let { save(it, amount) }
    }

    fun openTargetEdit(currentTarget: Int) {
        targetEdit.value = TargetEditUi(currentTarget = currentTarget, text = currentTarget.toString())
    }

    fun updateTargetText(text: String) = targetEdit.update { edit ->
        edit?.copy(text = text.filter(Char::isDigit).take(MAX_TARGET_DIGITS), error = null)
    }

    fun dismissTargetEdit() {
        targetEdit.value = null
    }

    fun confirmTargetChange() {
        val edit = targetEdit.value ?: return
        val newTarget = edit.text.toIntOrNull()
        if (newTarget == null) {
            targetEdit.value = edit.copy(error = TargetEditError.NOT_A_NUMBER)
            return
        }
        viewModelScope.launch {
            when (val result = changeTarget(newTarget)) {
                is Outcome.Success -> targetEdit.value = null

                is Outcome.Failure -> when (val error = result.error) {
                    is InvalidTarget -> targetEdit.value = edit.copy(error = error.error.toTargetError())
                    TargetUnchanged -> targetEdit.value = edit.copy(error = TargetEditError.UNCHANGED)
                    NoActiveGame -> targetEdit.value = null
                }
            }
        }
    }

    /**
     * Runs what the main button shows. Ending a round is ignored while a previous tap is still saving,
     * so a double tap never skips a round. Once anyone qualified the domain refuses to end the round.
     */
    fun onPrimaryAction(action: PrimaryAction) {
        when (action) {
            PrimaryAction.END_ROUND -> endCurrentRound()
            PrimaryAction.END_GAME -> Unit
        }
    }

    private fun endCurrentRound() {
        if (endRoundJob?.isActive == true) return
        endRoundJob = viewModelScope.launch { endRound() }
    }

    /** The score is written first; the card animates when the new total arrives from the database. */
    private fun save(entry: ScoreEntryUi, amount: Int) {
        viewModelScope.launch {
            when (val result = recordScore(PlayerId(entry.playerId), entry.type, amount)) {
                is Outcome.Success -> scoreEntry.value = null

                is Outcome.Failure -> when (val error = result.error) {
                    is InvalidAmount -> scoreEntry.value = entry.copy(error = error.error.toEntryError())

                    ScoreLimitReached -> scoreEntry.value = entry.copy(error = ScoreEntryError.SCORE_LIMIT)

                    // The game or the player is gone: the screen updates itself, nothing to fix in the sheet.
                    NoActiveGame, UnknownPlayer -> scoreEntry.value = null
                }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        val MAX_AMOUNT_DIGITS = ScoreAmount.ALLOWED.last.toString().length
        val MAX_TARGET_DIGITS = TargetScore.ALLOWED.last.toString().length
    }
}

private fun RangeError.toEntryError(): ScoreEntryError = when (this) {
    RangeError.TOO_LOW -> ScoreEntryError.TOO_LOW
    RangeError.TOO_HIGH -> ScoreEntryError.TOO_HIGH
}

private fun RangeError.toTargetError(): TargetEditError = when (this) {
    RangeError.TOO_LOW -> TargetEditError.TOO_LOW
    RangeError.TOO_HIGH -> TargetEditError.TOO_HIGH
}
