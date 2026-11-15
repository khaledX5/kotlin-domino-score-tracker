package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.model.Profile
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveKnownPlayersUseCase @Inject constructor(private val repository: GameRepository) {
    /** People who already played, the most recent first, at most [limit] of them, for quick picks at setup. */
    operator fun invoke(limit: Int = DEFAULT_LIMIT): Flow<List<Profile>> =
        repository.observeProfiles().map { it.take(limit) }

    companion object {
        const val DEFAULT_LIMIT = 8
    }
}
