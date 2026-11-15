package io.github.khaledx5.dominoscore.domain

import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.Round
import io.github.khaledx5.dominoscore.domain.model.RoundId
import io.github.khaledx5.dominoscore.domain.model.RoundStatus
import io.github.khaledx5.dominoscore.domain.model.ScoreAmount
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

val T0: Instant = Instant.parse("2026-11-15T18:00:00Z")

val fixedClock: Clock = Clock.fixed(T0, ZoneOffset.UTC)

fun <T> Outcome<T, *>.valueOrFail(): T = when (this) {
    is Outcome.Success -> value
    is Outcome.Failure -> throw AssertionError("Expected success but was $error")
}

fun name(raw: String): PlayerName = PlayerName.of(raw).valueOrFail()

fun target(value: Int): TargetScore = TargetScore.of(value).valueOrFail()

fun amount(value: Int): ScoreAmount = ScoreAmount.of(value).valueOrFail()

fun player(seat: Int, score: Int = 0, name: String = "Player $seat"): Player = Player(
    id = PlayerId(seat.toLong()),
    seat = seat,
    name = name(name),
    score = score,
    colorIndex = seat,
)

fun scores(vararg values: Int): List<Player> = values.mapIndexed { seat, score -> player(seat, score) }

fun game(players: List<Player> = scores(0, 0), target: Int = 150): Game = Game(
    id = GameId(1),
    target = target(target),
    status = GameStatus.ACTIVE,
    players = players,
    currentRound = Round(RoundId(1), number = 1, status = RoundStatus.IN_PROGRESS, startedAt = T0),
    startedAt = T0,
)
