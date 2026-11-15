package io.github.khaledx5.dominoscore.domain.repository

import io.github.khaledx5.dominoscore.domain.messages.PlayerMessageRecord
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import kotlinx.coroutines.flow.Flow

/** Storage of the messages players got. Kept per game and player id, and kept across app restarts. */
interface PlayerMessageRepository {

    /** Every message of the game, oldest first. */
    suspend fun history(gameId: GameId): List<PlayerMessageRecord>

    /** Each player's latest message in the game, emitted again after every save. */
    fun observeLatest(gameId: GameId): Flow<Map<PlayerId, PlayerMessageRecord>>

    /** Saves the messages of one event together. */
    suspend fun save(records: List<PlayerMessageRecord>)
}
