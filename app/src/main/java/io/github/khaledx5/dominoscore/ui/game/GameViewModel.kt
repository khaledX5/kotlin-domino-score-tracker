package io.github.khaledx5.dominoscore.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.khaledx5.dominoscore.audio.MusicController
import io.github.khaledx5.dominoscore.domain.command.GameCommand
import io.github.khaledx5.dominoscore.domain.command.GameCommandRunner
import io.github.khaledx5.dominoscore.domain.history.ObserveMovesUseCase
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerNameError
import io.github.khaledx5.dominoscore.domain.model.RangeError
import io.github.khaledx5.dominoscore.domain.model.ScoreAmount
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.rules.PrimaryAction
import io.github.khaledx5.dominoscore.domain.usecase.ChangeTargetUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ConfirmWinnerUseCase
import io.github.khaledx5.dominoscore.domain.usecase.EndRoundUseCase
import io.github.khaledx5.dominoscore.domain.usecase.InvalidAmount
import io.github.khaledx5.dominoscore.domain.usecase.InvalidName
import io.github.khaledx5.dominoscore.domain.usecase.InvalidTarget
import io.github.khaledx5.dominoscore.domain.usecase.NameTaken
import io.github.khaledx5.dominoscore.domain.usecase.NoActiveGame
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObservePlayerMessagesUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RenamePlayerUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ScoreLimitReached
import io.github.khaledx5.dominoscore.domain.usecase.TargetUnchanged
import io.github.khaledx5.dominoscore.domain.usecase.UnknownPlayer
import io.github.khaledx5.dominoscore.ui.components.withMessages
import io.github.khaledx5.dominoscore.ui.setup.NameFieldError
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GameViewModel @Inject constructor(
    observeActiveGame: ObserveActiveGameUseCase,
    observeMessages: ObservePlayerMessagesUseCase,
    observeMoves: ObserveMovesUseCase,
    private val recordScore: RecordScoreUseCase,
    private val changeTarget: ChangeTargetUseCase,
    private val endRound: EndRoundUseCase,
    private val endGameWith: ConfirmWinnerUseCase,
    private val renamePlayer: RenamePlayerUseCase,
    private val commands: GameCommandRunner,
    private val music: MusicController,
) : ViewModel() {

    private val scoreEntry = MutableStateFlow<ScoreEntryUi?>(null)
    private val targetEdit = MutableStateFlow<TargetEditUi?>(null)
    private val endedRound = MutableStateFlow<Int?>(null)
    private val winnerPick = MutableStateFlow<WinnerPickUi?>(null)
    private val rename = MutableStateFlow<RenameUi?>(null)

    /** While the winner is being saved the game disappears from the database: that is not "the game is gone". */
    private val finishing = MutableStateFlow<Finish>(Finish.None)
    private var endRoundJob: Job? = null

    private val changePlayers = MutableStateFlow(false)
    private val screenState = combine(
        combine(scoreEntry, targetEdit, endedRound, winnerPick, rename, ::ScreenState),
        changePlayers,
    ) { screen, enabled -> screen.copy(changePlayersEnabled = enabled) }

    /** The move query follows the game id, like the messages, so a score does not restart it. */
    private val moves = observeActiveGame()
        .map { it?.game?.id }
        .distinctUntilChanged()
        .flatMapLatest { gameId -> if (gameId == null) flowOf(emptyList()) else observeMoves(gameId) }

    val uiState: StateFlow<GameUiState> = combine(
        observeActiveGame().withMessages(observeMessages),
        moves,
        music.settings,
        screenState,
        finishing,
    ) { (snapshot, messages), allMoves, musicSettings, screen, finish ->
        if (snapshot == null) {
            when (finish) {
                is Finish.Done -> GameUiState.Finished(finish.gameId)
                Finish.Saving -> GameUiState.Loading
                Finish.None -> GameUiState.NoActiveGame
            }
        } else {
            GameUiState.Playing(
                game = snapshot.toGameUi(messages, allMoves),
                music = MusicButtonUi(visible = musicSettings.gameplayEnabled),
                scoreEntry = screen.scoreEntry,
                targetEdit = screen.targetEdit,
                endedRound = screen.endedRound,
                winnerPick = screen.winnerPick,
                rename = screen.rename,
                changePlayersEnabled = screen.changePlayersEnabled,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GameUiState.Loading)

    fun playMusic() = music.playGameplayMusic()

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
            when (val result = commands.run(GameCommand.ChangeTarget) { changeTarget(newTarget) }) {
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
            PrimaryAction.END_GAME -> openWinnerPick()
        }
    }

    private fun openWinnerPick() {
        val game = (uiState.value as? GameUiState.Playing)?.game ?: return
        if (game.winnerCandidates.isEmpty()) return
        winnerPick.value = WinnerPickUi(
            candidates = game.winnerCandidates,
            tieAtTop = game.suggestedWinnerId == null,
            selectedId = game.suggestedWinnerId,
        )
    }

    fun selectWinner(playerId: Long) = winnerPick.update { pick ->
        if (pick == null || pick.candidates.none { it.playerId == playerId }) pick else pick.copy(selectedId = playerId)
    }

    /** Cancel: the game stays active, nothing is saved, no music plays. */
    fun dismissWinnerPick() {
        winnerPick.value = null
    }

    /**
     * The game ends only here, with the player the table picked. Then the gameplay tune stops, the
     * winner tune plays if it is switched on, and the screen opens the celebration.
     */
    fun confirmWinner() {
        val pick = winnerPick.value ?: return
        val winnerId = pick.selectedId ?: return
        val gameId = (uiState.value as? GameUiState.Playing)?.game?.gameId ?: return
        finishing.value = Finish.Saving
        viewModelScope.launch {
            val result = endGameWith(PlayerId(winnerId))
            winnerPick.value = null
            if (result is Outcome.Success) {
                music.celebrateWinner()
                finishing.value = Finish.Done(gameId)
            } else {
                // Refused (the target was raised meanwhile): the dialog closes and the game goes on.
                finishing.value = Finish.None
            }
        }
    }

    private fun endCurrentRound() {
        if (endRoundJob?.isActive == true) return
        endRoundJob = viewModelScope.launch {
            val result = commands.run(GameCommand.EndRound) { endRound() }
            if (result is Outcome.Success) endedRound.value = result.value
        }
    }

    fun onRoundResultsShown() {
        endedRound.value = null
    }

    /** Turning "Change players" off also closes a rename that is still open. */
    fun toggleChangePlayers() {
        val enabled = !changePlayers.value
        changePlayers.value = enabled
        if (!enabled) rename.value = null
    }

    /** Opens the rename dialog, only while "Change players" is on, so a stray tap never edits a name. */
    fun openRename(playerId: Long, currentName: String) {
        if (!changePlayers.value) return
        rename.value = RenameUi(playerId = playerId, currentName = currentName, text = currentName)
    }

    fun updateRenameText(text: String) = rename.update { it?.copy(text = text, error = null) }

    fun dismissRename() {
        rename.value = null
    }

    /** The domain checks the name; its answer becomes the field error. */
    fun confirmRename() {
        val edit = rename.value ?: return
        viewModelScope.launch {
            when (val result = renamePlayer(PlayerId(edit.playerId), edit.text)) {
                is Outcome.Success -> rename.value = null

                is Outcome.Failure -> when (val error = result.error) {
                    is InvalidName -> rename.value = edit.copy(error = error.error.toFieldError())
                    NameTaken -> rename.value = edit.copy(error = NameFieldError.DUPLICATE)
                    NoActiveGame, UnknownPlayer -> rename.value = null
                }
            }
        }
    }

    /** The score is written first; the card animates when the new total arrives from the database. */
    private fun save(entry: ScoreEntryUi, amount: Int) {
        viewModelScope.launch {
            val player = PlayerId(entry.playerId)
            when (
                val result = commands.run(GameCommand.Score(player, entry.type, amount)) {
                    recordScore(player, entry.type, amount)
                }
            ) {
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

    private sealed interface Finish {
        data object None : Finish

        data object Saving : Finish

        data class Done(val gameId: Long) : Finish
    }

    /** What only this screen knows: the open sheet or dialog and a round that was just ended. */
    private data class ScreenState(
        val scoreEntry: ScoreEntryUi?,
        val targetEdit: TargetEditUi?,
        val endedRound: Int?,
        val winnerPick: WinnerPickUi?,
        val rename: RenameUi?,
        val changePlayersEnabled: Boolean = false,
    )

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

private fun PlayerNameError.toFieldError(): NameFieldError = when (this) {
    PlayerNameError.BLANK -> NameFieldError.BLANK
    PlayerNameError.TOO_LONG -> NameFieldError.TOO_LONG
}
