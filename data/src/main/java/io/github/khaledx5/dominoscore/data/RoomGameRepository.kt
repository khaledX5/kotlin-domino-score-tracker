package io.github.khaledx5.dominoscore.data

import androidx.room.withTransaction
import io.github.khaledx5.dominoscore.data.local.DominoDatabase
import io.github.khaledx5.dominoscore.data.local.entity.GameEntity
import io.github.khaledx5.dominoscore.data.local.entity.GameEventEntity
import io.github.khaledx5.dominoscore.data.local.entity.GameEventType
import io.github.khaledx5.dominoscore.data.local.entity.PlayerEntity
import io.github.khaledx5.dominoscore.data.local.entity.RoundEntity
import io.github.khaledx5.dominoscore.data.local.toDomain
import io.github.khaledx5.dominoscore.data.local.toEntity
import io.github.khaledx5.dominoscore.domain.messages.ScoreLog
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.RoundId
import io.github.khaledx5.dominoscore.domain.model.RoundStatus
import io.github.khaledx5.dominoscore.domain.model.ScoreOperation
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.repository.NewGame
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [GameRepository] on Room. Every write runs in one SQLite transaction, so a killed process
 * either keeps the whole change or none of it. Broken preconditions (a round that is not in
 * progress, a player of another game) throw: the use cases check the rules before calling in.
 */
class RoomGameRepository(private val database: DominoDatabase) : GameRepository {

    private val dao = database.gameDao()

    override fun observeActiveGame(): Flow<Game?> = dao.observeActiveGame().map { it?.toDomain() }

    override suspend fun findActiveGame(): Game? = dao.findActiveGame()?.toDomain()

    override suspend fun findGame(gameId: GameId): Game? = dao.findGame(gameId.value)?.toDomain()

    override suspend fun findScoreLog(gameId: GameId): ScoreLog = database.withTransaction {
        ScoreLog(
            completedRounds = dao.findCompletedRounds(gameId.value).map { it.toDomain() },
            operations = dao.findScoreOperations(gameId.value).map { it.toDomain() },
        )
    }

    override suspend fun startGame(newGame: NewGame): GameId = database.withTransaction {
        dao.abandonActiveGame()
        val gameId = dao.insertGame(
            GameEntity(targetScore = newGame.target.value, status = GameStatus.ACTIVE, startedAt = newGame.startedAt),
        )
        dao.insertPlayers(
            newGame.playerNames.mapIndexed { seat, name ->
                PlayerEntity(gameId = gameId, seat = seat, name = name.value, score = 0, colorIndex = seat)
            },
        )
        dao.insertRound(newRound(gameId, number = 1, startedAt = newGame.startedAt))
        GameId(gameId)
    }

    override suspend fun recordScore(gameId: GameId, operation: ScoreOperation) = database.withTransaction {
        requirePlayerOf(gameId, operation.playerId)
        dao.insertScoreOperation(operation.toEntity(gameId))
        dao.updateScore(operation.playerId.value, operation.newScore)
    }

    override suspend fun changeTarget(gameId: GameId, newTarget: TargetScore, changedAt: Instant) =
        database.withTransaction {
            val game = checkNotNull(dao.findGameRow(gameId.value)) { "Game ${gameId.value} not found" }
            dao.updateTarget(gameId.value, newTarget.value)
            dao.insertEvent(
                GameEventEntity(
                    gameId = gameId.value,
                    roundId = dao.findRoundInProgress(gameId.value)?.id,
                    type = GameEventType.TARGET_CHANGED,
                    oldValue = game.targetScore.toString(),
                    newValue = newTarget.value.toString(),
                    createdAt = changedAt,
                ),
            )
        }

    override suspend fun completeRound(gameId: GameId, roundId: RoundId, completedAt: Instant) =
        database.withTransaction {
            val round = requireRoundInProgress(gameId, roundId)
            dao.completeRound(round.id, completedAt)
            dao.insertEvent(
                GameEventEntity(
                    gameId = gameId.value,
                    roundId = round.id,
                    type = GameEventType.ROUND_COMPLETED,
                    createdAt = completedAt,
                ),
            )
            dao.insertRound(newRound(gameId.value, number = round.number + 1, startedAt = completedAt))
            Unit
        }

    override suspend fun completeGame(gameId: GameId, winnerId: PlayerId, completedAt: Instant) =
        database.withTransaction {
            requirePlayerOf(gameId, winnerId)
            val round = checkNotNull(dao.findRoundInProgress(gameId.value)) { "Game ${gameId.value} has no round" }
            dao.completeRound(round.id, completedAt)
            check(dao.completeGame(gameId.value, winnerId.value, completedAt) == 1) {
                "Game ${gameId.value} is not active"
            }
            dao.insertEvent(
                GameEventEntity(
                    gameId = gameId.value,
                    roundId = round.id,
                    type = GameEventType.WINNER_CONFIRMED,
                    playerId = winnerId.value,
                    createdAt = completedAt,
                ),
            )
            dao.insertEvent(
                GameEventEntity(
                    gameId = gameId.value,
                    roundId = round.id,
                    type = GameEventType.GAME_COMPLETED,
                    createdAt = completedAt,
                ),
            )
        }

    override suspend fun renamePlayer(gameId: GameId, playerId: PlayerId, newName: PlayerName, renamedAt: Instant) =
        database.withTransaction {
            val player = requirePlayerOf(gameId, playerId)
            dao.updateName(playerId.value, newName.value)
            dao.insertEvent(
                GameEventEntity(
                    gameId = gameId.value,
                    roundId = dao.findRoundInProgress(gameId.value)?.id,
                    type = GameEventType.PLAYER_RENAMED,
                    oldValue = player.name,
                    newValue = newName.value,
                    playerId = playerId.value,
                    createdAt = renamedAt,
                ),
            )
        }

    private suspend fun requirePlayerOf(gameId: GameId, playerId: PlayerId): PlayerEntity {
        val player = dao.findPlayer(playerId.value)
        check(player != null && player.gameId == gameId.value) {
            "Player ${playerId.value} is not part of game ${gameId.value}"
        }
        return player
    }

    private suspend fun requireRoundInProgress(gameId: GameId, roundId: RoundId): RoundEntity {
        val round = dao.findRoundInProgress(gameId.value)
        check(round != null && round.id == roundId.value) {
            "Round ${roundId.value} is not in progress in game ${gameId.value}"
        }
        return round
    }

    private fun newRound(gameId: Long, number: Int, startedAt: Instant) =
        RoundEntity(gameId = gameId, number = number, status = RoundStatus.IN_PROGRESS, startedAt = startedAt)
}
