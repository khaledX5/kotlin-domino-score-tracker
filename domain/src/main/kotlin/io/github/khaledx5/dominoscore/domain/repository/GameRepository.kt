package io.github.khaledx5.dominoscore.domain.repository

import io.github.khaledx5.dominoscore.domain.history.ScoreMove
import io.github.khaledx5.dominoscore.domain.messages.ScoreLog
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.Profile
import io.github.khaledx5.dominoscore.domain.model.RoundId
import io.github.khaledx5.dominoscore.domain.model.ScoreOperation
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.rules.RankedPlayer
import java.time.Instant
import kotlinx.coroutines.flow.Flow

data class NewGame(
    val target: TargetScore,
    /** Names in seat order: the first name sits on seat 0. */
    val playerNames: List<PlayerName>,
    val startedAt: Instant,
)

/**
 * Storage contract of the game. Business rules are checked by the use cases before calling it;
 * every write method is one transaction, so a killed app never leaves a half-written change.
 */
interface GameRepository {

    /** Emits the active game (or null when there is none) and again after every change to it. */
    fun observeActiveGame(): Flow<Game?>

    suspend fun findActiveGame(): Game?

    /** Every score move of [gameId] with its round number, newest first, emitted again after each new one. */
    fun observeMoves(gameId: GameId): Flow<List<ScoreMove>>

    /** Every COMPLETED or ABANDONED game, newest first, emitted again after every change. */
    fun observeFinishedGames(): Flow<List<Game>>

    /**
     * Every person who sat in any game, the one seated most recently first (the latest game, then
     * its last seat first), emitted again after every new game.
     */
    fun observeProfiles(): Flow<List<Profile>>

    /** Any game, active or finished, or null when there is no game with [gameId]. */
    suspend fun findGame(gameId: GameId): Game?

    /** The completed rounds (by number) and the score log (in the order it was written) of [gameId]. */
    suspend fun findScoreLog(gameId: GameId): ScoreLog

    /**
     * Creates the game with round 1 in progress. A game that is still active becomes ABANDONED in the
     * same transaction. Each player is linked to the profile with the same [PlayerName.key], or to a
     * new profile, so the same name at the table is the same person in the statistics.
     */
    suspend fun startGame(newGame: NewGame): GameId

    /** Appends [operation] to the score log and sets the player score to its new score. */
    suspend fun recordScore(gameId: GameId, operation: ScoreOperation)

    /** Saves the new target and a TARGET_CHANGED event with the old and new values. */
    suspend fun changeTarget(gameId: GameId, newTarget: TargetScore, changedAt: Instant)

    /** Completes [roundId] with a ROUND_COMPLETED event and opens the next round. Scores are kept. */
    suspend fun completeRound(gameId: GameId, roundId: RoundId, completedAt: Instant)

    /**
     * Saves the winner, completes the current round and the game, with WINNER_CONFIRMED and
     * GAME_COMPLETED events. [finalStandings] (one per player) are frozen with it: history and
     * statistics read them as they were, even if the ranking rules change in a later version.
     */
    suspend fun completeGame(
        gameId: GameId,
        winnerId: PlayerId,
        finalStandings: List<RankedPlayer>,
        completedAt: Instant,
    )

    /** Updates the name only: the player id stays, so the score log keeps pointing at the same player. */
    suspend fun renamePlayer(gameId: GameId, playerId: PlayerId, newName: PlayerName, renamedAt: Instant)
}
