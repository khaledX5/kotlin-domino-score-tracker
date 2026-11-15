package io.github.khaledx5.dominoscore.domain.model

import java.time.Instant

enum class RoundStatus {
    IN_PROGRESS,
    COMPLETED,
}

data class Round(
    val id: RoundId,
    val number: Int,
    val status: RoundStatus,
    val startedAt: Instant,
    val completedAt: Instant? = null,
)
