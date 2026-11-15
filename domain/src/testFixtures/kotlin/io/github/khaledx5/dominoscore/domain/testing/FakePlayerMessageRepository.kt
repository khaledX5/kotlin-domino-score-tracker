package io.github.khaledx5.dominoscore.domain.testing

import io.github.khaledx5.dominoscore.domain.messages.PlayerMessageRecord
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.repository.PlayerMessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [PlayerMessageRepository] for use case and ViewModel tests. */
class FakePlayerMessageRepository : PlayerMessageRepository {

    private val records = MutableStateFlow<List<PlayerMessageRecord>>(emptyList())

    val all: List<PlayerMessageRecord> get() = records.value

    override suspend fun history(gameId: GameId): List<PlayerMessageRecord> =
        records.value.filter { it.gameId == gameId }

    override fun observeLatest(gameId: GameId): Flow<Map<PlayerId, PlayerMessageRecord>> = records
        .map { list -> list.filter { it.gameId == gameId }.associateBy { it.playerId } }
        .distinctUntilChanged()

    override suspend fun save(records: List<PlayerMessageRecord>) {
        if (records.isNotEmpty()) this.records.update { it + records }
    }
}
