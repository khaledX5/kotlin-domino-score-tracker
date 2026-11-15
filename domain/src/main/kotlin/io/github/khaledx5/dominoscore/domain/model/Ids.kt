package io.github.khaledx5.dominoscore.domain.model

@JvmInline
value class GameId(val value: Long)

@JvmInline
value class PlayerId(val value: Long)

@JvmInline
value class RoundId(val value: Long)

/** One person across games: every seat with the same name (without case) shares it. */
@JvmInline
value class ProfileId(val value: Long)
