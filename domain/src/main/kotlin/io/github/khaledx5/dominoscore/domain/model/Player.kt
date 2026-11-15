package io.github.khaledx5.dominoscore.domain.model

/**
 * A seat at the table. [score] is cumulative over the whole game, signed and never floored:
 * penalties can take it below zero.
 */
data class Player(val id: PlayerId, val seat: Int, val name: PlayerName, val score: Int, val colorIndex: Int)
