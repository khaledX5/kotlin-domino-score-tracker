package io.github.khaledx5.dominoscore.domain.usecase

import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.Round

/** An active game always has a round in progress; anything else is a storage bug, not a user error. */
internal fun Game.roundInProgress(): Round = checkNotNull(currentRound) {
    "Active game ${id.value} has no round in progress"
}
