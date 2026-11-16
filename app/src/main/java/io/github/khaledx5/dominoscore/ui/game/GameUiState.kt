package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.runtime.Immutable
import io.github.khaledx5.dominoscore.domain.history.ScoreMove
import io.github.khaledx5.dominoscore.domain.messages.RankingMessage
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.rules.GameSnapshot
import io.github.khaledx5.dominoscore.domain.rules.PrimaryAction
import io.github.khaledx5.dominoscore.domain.rules.WinnerSelection
import io.github.khaledx5.dominoscore.ui.components.StandingUi
import io.github.khaledx5.dominoscore.ui.components.toStandings

sealed interface GameUiState {
    data object Loading : GameUiState

    /** The game ended or was replaced: the screen closes itself. */
    data object NoActiveGame : GameUiState

    /** The table confirmed the winner of [gameId]: the screen opens the celebration. */
    data class Finished(val gameId: Long) : GameUiState

    data class Playing(
        val game: GameUi,
        val music: MusicButtonUi,
        val scoreEntry: ScoreEntryUi? = null,
        val targetEdit: TargetEditUi? = null,
        /** Set once a round was ended here: the screen opens its results, then clears it. */
        val endedRound: Int? = null,
        val winnerPick: WinnerPickUi? = null,
        val rename: RenameUi? = null,
        /** Names can be changed only while the table turned "Change players" on. Off on every visit. */
        val changePlayersEnabled: Boolean = false,
    ) : GameUiState
}

/**
 * [players] stay in seat order, so the Add and Penalty buttons never move under a finger;
 * [standings] are the same players in ranking order for the live ranking card.
 */
@Immutable
data class GameUi(
    val roundNumber: Int,
    val gameId: Long = 0,
    val target: Int,
    val players: List<StandingUi>,
    val standings: List<StandingUi>,
    val primaryAction: PrimaryAction = PrimaryAction.END_ROUND,
    /** Who the table may confirm, from the domain's winner selection. Empty while nobody qualified. */
    val winnerCandidates: List<WinnerCandidateUi> = emptyList(),
    val suggestedWinnerId: Long? = null,
    /** The newest moves first, at most [RECENT_MOVES]. */
    val recentMoves: List<MoveUi> = emptyList(),
) {
    /** Players at or above the target, in ranking order. Empty while nobody qualified. */
    val qualifiedNames: List<String> get() = standings.filter { it.qualified }.map { it.name }
}

enum class ScoreEntryError {
    NOT_A_NUMBER,
    TOO_LOW,
    TOO_HIGH,

    /** The new total would not fit: a safety net, far above any real game. */
    SCORE_LIMIT,
}

/** The open Add or Penalty sheet for one player. */
@Immutable
data class ScoreEntryUi(
    val playerId: Long,
    val playerName: String,
    val type: ScoreOperationType,
    val amountText: String = "",
    val error: ScoreEntryError? = null,
) {
    companion object {
        /** Common domino hands, one tap each. */
        val QUICK_AMOUNTS = listOf(5, 10, 15, 20, 25, 50)
    }
}

enum class TargetEditError {
    NOT_A_NUMBER,
    TOO_LOW,
    TOO_HIGH,
    UNCHANGED,
}

/** The open "change target" dialog. Nothing changes until the table confirms. */
@Immutable
data class TargetEditUi(val currentTarget: Int, val text: String, val error: TargetEditError? = null)

/** The music button is hidden when gameplay music is switched off in the settings. */
@Immutable
data class MusicButtonUi(val visible: Boolean)

/** How many moves the game screen shows under the live ranking. */
const val RECENT_MOVES = 6

internal fun GameSnapshot.toGameUi(
    messages: Map<PlayerId, RankingMessage> = emptyMap(),
    moves: List<ScoreMove> = emptyList(),
): GameUi {
    val standings = toStandings(messages)
    return GameUi(
        roundNumber = checkNotNull(game.currentRound) { "An active game always has a round in progress" }.number,
        gameId = game.id.value,
        target = game.target.value,
        players = standings.sortedBy { standing -> game.players.indexOfFirst { it.id.value == standing.playerId } },
        standings = standings,
        primaryAction = primaryAction,
        winnerCandidates = winnerSelection.candidates.map { WinnerCandidateUi(it.id.value, it.name.value, it.score) },
        suggestedWinnerId = (winnerSelection as? WinnerSelection.Suggested)?.suggested?.id?.value,
        recentMoves = moves.take(RECENT_MOVES).mapNotNull { it.toUi(game.players) },
    )
}
