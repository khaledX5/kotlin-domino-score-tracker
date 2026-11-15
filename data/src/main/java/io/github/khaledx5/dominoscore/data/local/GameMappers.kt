package io.github.khaledx5.dominoscore.data.local

import io.github.khaledx5.dominoscore.data.local.entity.PlayerEntity
import io.github.khaledx5.dominoscore.data.local.entity.ProfileEntity
import io.github.khaledx5.dominoscore.data.local.entity.RoundEntity
import io.github.khaledx5.dominoscore.data.local.entity.ScoreOperationEntity
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.Profile
import io.github.khaledx5.dominoscore.domain.model.ProfileId
import io.github.khaledx5.dominoscore.domain.model.Round
import io.github.khaledx5.dominoscore.domain.model.RoundId
import io.github.khaledx5.dominoscore.domain.model.RoundStatus
import io.github.khaledx5.dominoscore.domain.model.ScoreAmount
import io.github.khaledx5.dominoscore.domain.model.ScoreOperation
import io.github.khaledx5.dominoscore.domain.model.TargetScore

internal fun GameWithDetails.toDomain(): Game = Game(
    id = GameId(game.id),
    target = TargetScore.of(game.targetScore).requireStored("target score of game ${game.id}"),
    status = game.status,
    players = players.sortedBy { it.seat }.map { it.toDomain() },
    currentRound = rounds.firstOrNull { it.status == RoundStatus.IN_PROGRESS }?.toDomain(),
    startedAt = game.startedAt,
    winnerId = game.winnerPlayerId?.let(::PlayerId),
    completedAt = game.completedAt,
)

internal fun PlayerEntity.toDomain(): Player = Player(
    id = PlayerId(id),
    seat = seat,
    name = PlayerName.of(name).requireStored("name of player $id"),
    score = score,
    colorIndex = colorIndex,
)

internal fun ProfileEntity.toDomain(): Profile =
    Profile(id = ProfileId(id), name = PlayerName.of(name).requireStored("name of profile $id"))

internal fun RoundEntity.toDomain(): Round = Round(
    id = RoundId(id),
    number = number,
    status = status,
    startedAt = startedAt,
    completedAt = completedAt,
)

internal fun ScoreOperation.toEntity(gameId: GameId): ScoreOperationEntity = ScoreOperationEntity(
    gameId = gameId.value,
    roundId = roundId.value,
    playerId = playerId.value,
    type = type,
    amount = amount.value,
    previousScore = previousScore,
    newScore = newScore,
    createdAt = createdAt,
)

internal fun ScoreOperationEntity.toDomain(): ScoreOperation = ScoreOperation(
    roundId = RoundId(roundId),
    playerId = PlayerId(playerId),
    type = type,
    amount = ScoreAmount.of(amount).requireStored("amount of score operation $id"),
    previousScore = previousScore,
    newScore = newScore,
    createdAt = createdAt,
)

/** Rows are only written after domain validation, so an invalid value here means a corrupt database. */
private fun <T> Outcome<T, *>.requireStored(what: String): T = when (this) {
    is Outcome.Success -> value
    is Outcome.Failure -> error("Stored $what is invalid: $error")
}
