package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.model.PlayerNameError
import io.github.khaledx5.dominoscore.domain.model.RangeError
import io.github.khaledx5.dominoscore.domain.rules.RosterIssue

sealed interface StartGameError

sealed interface RecordScoreError

sealed interface ChangeTargetError

sealed interface EndRoundError

sealed interface ConfirmWinnerError

sealed interface RenamePlayerError

/** No game is in progress, for example because it ended or was replaced on another screen. */
data object NoActiveGame :
    RecordScoreError,
    ChangeTargetError,
    EndRoundError,
    ConfirmWinnerError,
    RenamePlayerError

/** The player is not part of the active game. */
data object UnknownPlayer :
    RecordScoreError,
    RenamePlayerError

data class InvalidTarget(val error: RangeError) :
    StartGameError,
    ChangeTargetError

data class InvalidRoster(val issues: List<RosterIssue>) : StartGameError

/** A color outside the palette, a color used twice, or not one color per player. */
data object InvalidColors : StartGameError

/** Starting a new game needs the table to confirm, because the active one will be abandoned. */
data object ActiveGameExists : StartGameError

data class InvalidAmount(val error: RangeError) : RecordScoreError

/** The new score would not fit in an Int. */
data object ScoreLimitReached : RecordScoreError

data object TargetUnchanged : ChangeTargetError

/** Someone reached the target: the table must confirm a winner or raise the target instead. */
data object PlayerQualified : EndRoundError

data object NobodyQualified : ConfirmWinnerError

/** The player may not be confirmed: not qualified, or not one of the players tied at the top. */
data object NotACandidate : ConfirmWinnerError

data class InvalidName(val error: PlayerNameError) : RenamePlayerError

data object NameTaken : RenamePlayerError
