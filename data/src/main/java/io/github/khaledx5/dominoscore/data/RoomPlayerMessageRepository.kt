package io.github.khaledx5.dominoscore.data

import io.github.khaledx5.dominoscore.data.local.DominoDatabase
import io.github.khaledx5.dominoscore.data.local.entity.PlayerMessageEntity
import io.github.khaledx5.dominoscore.domain.messages.PlayerMessageRecord
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.RoundId
import io.github.khaledx5.dominoscore.domain.repository.PlayerMessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** [PlayerMessageRepository] on Room: one table, inserted in one statement per event. */
class RoomPlayerMessageRepository(database: DominoDatabase) : PlayerMessageRepository {

    private val dao = database.playerMessageDao()

    override suspend fun history(gameId: GameId): List<PlayerMessageRecord> =
        dao.findForGame(gameId.value).map { it.toDomain() }

    override fun observeLatest(gameId: GameId): Flow<Map<PlayerId, PlayerMessageRecord>> = dao
        .observeLatestForGame(gameId.value)
        .map { rows -> rows.associate { PlayerId(it.playerId) to it.toDomain() } }
        .distinctUntilChanged()

    override suspend fun save(records: List<PlayerMessageRecord>) {
        if (records.isNotEmpty()) dao.insertAll(records.map { it.toEntity() })
    }
}

private fun PlayerMessageEntity.toDomain() = PlayerMessageRecord(
    gameId = GameId(gameId),
    playerId = PlayerId(playerId),
    roundId = RoundId(roundId),
    messageId = messageId,
    category = category,
    trigger = trigger,
    createdAt = createdAt,
)

private fun PlayerMessageRecord.toEntity() = PlayerMessageEntity(
    gameId = gameId.value,
    playerId = playerId.value,
    roundId = roundId.value,
    messageId = messageId,
    category = category,
    trigger = trigger,
    createdAt = createdAt,
)
