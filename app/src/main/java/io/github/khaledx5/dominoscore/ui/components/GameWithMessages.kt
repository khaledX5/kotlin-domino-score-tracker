package io.github.khaledx5.dominoscore.ui.components

import io.github.khaledx5.dominoscore.domain.messages.RankingMessage
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.rules.GameSnapshot
import io.github.khaledx5.dominoscore.domain.usecase.ObservePlayerMessagesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** The active game and each player's latest line. Only reads: showing a screen never creates a message. */
data class GameWithMessages(val snapshot: GameSnapshot?, val messages: Map<PlayerId, RankingMessage>)

/** The message query follows the game id, so a score change does not restart it. */
@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<GameSnapshot?>.withMessages(observeMessages: ObservePlayerMessagesUseCase): Flow<GameWithMessages> {
    val messages = map { it?.game?.id }
        .distinctUntilChanged()
        .flatMapLatest { gameId -> if (gameId == null) flowOf(emptyMap()) else observeMessages(gameId) }
    return combine(this, messages, ::GameWithMessages)
}
