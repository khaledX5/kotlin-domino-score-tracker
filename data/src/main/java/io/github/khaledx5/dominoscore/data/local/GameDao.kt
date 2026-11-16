package io.github.khaledx5.dominoscore.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.github.khaledx5.dominoscore.data.local.entity.FinalStandingEntity
import io.github.khaledx5.dominoscore.data.local.entity.GameEntity
import io.github.khaledx5.dominoscore.data.local.entity.GameEventEntity
import io.github.khaledx5.dominoscore.data.local.entity.PlayerEntity
import io.github.khaledx5.dominoscore.data.local.entity.ProfileEntity
import io.github.khaledx5.dominoscore.data.local.entity.RoundEntity
import io.github.khaledx5.dominoscore.data.local.entity.ScoreOperationEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Single-statement reads and writes. Multi-step changes are composed in a transaction by the repository. */
@Dao
interface GameDao {

    @Transaction
    @Query("SELECT * FROM games WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActiveGame(): Flow<GameWithDetails?>

    @Transaction
    @Query("SELECT * FROM games WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun findActiveGame(): GameWithDetails?

    @Transaction
    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun findGame(gameId: Long): GameWithDetails?

    @Query(
        "SELECT score_operations.*, rounds.number AS round_number FROM score_operations " +
            "INNER JOIN rounds ON rounds.id = score_operations.round_id " +
            "WHERE score_operations.game_id = :gameId ORDER BY score_operations.id DESC",
    )
    fun observeMoves(gameId: Long): Flow<List<MoveRow>>

    @Transaction
    @Query("SELECT * FROM games WHERE status = 'COMPLETED' ORDER BY completed_at DESC, id DESC")
    fun observeCompletedGames(): Flow<List<CompletedGameRow>>

    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun findGameRow(gameId: Long): GameEntity?

    @Query("SELECT * FROM players WHERE id = :playerId")
    suspend fun findPlayer(playerId: Long): PlayerEntity?

    @Query("SELECT * FROM rounds WHERE game_id = :gameId AND status = 'IN_PROGRESS' LIMIT 1")
    suspend fun findRoundInProgress(gameId: Long): RoundEntity?

    @Query("SELECT * FROM rounds WHERE game_id = :gameId AND status = 'COMPLETED' ORDER BY number")
    suspend fun findCompletedRounds(gameId: Long): List<RoundEntity>

    @Query("SELECT * FROM score_operations WHERE game_id = :gameId ORDER BY id")
    suspend fun findScoreOperations(gameId: Long): List<ScoreOperationEntity>

    @Query("SELECT * FROM game_events WHERE game_id = :gameId ORDER BY id")
    suspend fun findEvents(gameId: Long): List<GameEventEntity>

    @Query("SELECT * FROM profiles WHERE name_key = :nameKey")
    suspend fun findProfile(nameKey: String): ProfileEntity?

    /** Player ids only grow, so the newest seat of a profile tells when it was last at the table. */
    @Query(
        "SELECT profiles.* FROM profiles INNER JOIN players ON players.profile_id = profiles.id " +
            "GROUP BY profiles.id ORDER BY MAX(players.id) DESC",
    )
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY id")
    suspend fun findProfiles(): List<ProfileEntity>

    @Query(
        "SELECT final_standings.* FROM final_standings " +
            "INNER JOIN players ON players.id = final_standings.player_id " +
            "WHERE players.game_id = :gameId ORDER BY final_standings.rank, players.seat",
    )
    suspend fun findFinalStandings(gameId: Long): List<FinalStandingEntity>

    @Insert
    suspend fun insertGame(game: GameEntity): Long

    @Insert
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Insert
    suspend fun insertFinalStandings(standings: List<FinalStandingEntity>)

    @Insert
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Insert
    suspend fun insertRound(round: RoundEntity): Long

    @Insert
    suspend fun insertScoreOperation(operation: ScoreOperationEntity)

    @Insert
    suspend fun insertEvent(event: GameEventEntity)

    @Query("UPDATE games SET status = 'ABANDONED' WHERE status = 'ACTIVE'")
    suspend fun abandonActiveGame(): Int

    @Query("UPDATE players SET score = :score WHERE id = :playerId")
    suspend fun updateScore(playerId: Long, score: Int)

    @Query("UPDATE players SET name = :name WHERE id = :playerId")
    suspend fun updateName(playerId: Long, name: String)

    @Query("UPDATE games SET target_score = :targetScore WHERE id = :gameId")
    suspend fun updateTarget(gameId: Long, targetScore: Int)

    /** Returns the number of rows changed: 0 means the round was not in progress. */
    @Query(
        "UPDATE rounds SET status = 'COMPLETED', completed_at = :completedAt " +
            "WHERE id = :roundId AND status = 'IN_PROGRESS'",
    )
    suspend fun completeRound(roundId: Long, completedAt: Instant): Int

    /** Returns the number of rows changed: 0 means the game was not active. */
    @Query(
        "UPDATE games SET status = 'COMPLETED', winner_player_id = :winnerId, completed_at = :completedAt " +
            "WHERE id = :gameId AND status = 'ACTIVE'",
    )
    suspend fun completeGame(gameId: Long, winnerId: Long, completedAt: Instant): Int
}
