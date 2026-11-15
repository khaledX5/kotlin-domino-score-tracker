package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.messages.MessageLibrarySource
import io.github.khaledx5.dominoscore.domain.messages.RankingMessage
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.repository.PlayerMessageRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservePlayerMessagesUseCase @Inject constructor(
    private val messages: PlayerMessageRepository,
    private val librarySource: MessageLibrarySource,
) {
    /** Each player's latest line in [gameId], with its text. A line no longer in the library is left out. */
    operator fun invoke(gameId: GameId): Flow<Map<PlayerId, RankingMessage>> =
        messages.observeLatest(gameId).map { latest ->
            val library = librarySource.load()
            latest.mapNotNull { (playerId, record) -> library.find(record.messageId)?.let { playerId to it } }.toMap()
        }
}
