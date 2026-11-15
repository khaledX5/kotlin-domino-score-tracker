package io.github.khaledx5.dominoscore.domain.model

import java.time.Instant

enum class ScoreOperationType {
    ADD,
    PENALTY,
}

/** One append-only entry of the score log. Keeps both scores so history never has to replay the log. */
data class ScoreOperation(
    val roundId: RoundId,
    val playerId: PlayerId,
    val type: ScoreOperationType,
    val amount: ScoreAmount,
    val previousScore: Int,
    val newScore: Int,
    val createdAt: Instant,
)
