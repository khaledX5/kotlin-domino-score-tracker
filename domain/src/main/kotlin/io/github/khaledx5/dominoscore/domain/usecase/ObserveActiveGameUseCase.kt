package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.rules.GameSnapshot
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveActiveGameUseCase @Inject constructor(private val repository: GameRepository) {
    /** The active game with its ranking, primary action and winner selection, or null when none is active. */
    operator fun invoke(): Flow<GameSnapshot?> = repository.observeActiveGame().map { game ->
        game?.let(GameSnapshot::of)
    }
}
