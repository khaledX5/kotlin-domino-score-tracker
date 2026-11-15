package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.rules.GameSnapshot
import javax.inject.Inject

class GetGameUseCase @Inject constructor(private val repository: GameRepository) {
    /** Any game with its ranking, for the winner screen and history; null when it does not exist. */
    suspend operator fun invoke(gameId: GameId): GameSnapshot? = repository.findGame(gameId)?.let(GameSnapshot::of)
}
