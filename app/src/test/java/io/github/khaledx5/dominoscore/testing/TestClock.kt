package io.github.khaledx5.dominoscore.testing

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

val testClock: Clock = Clock.fixed(Instant.parse("2026-11-15T18:00:00Z"), ZoneOffset.UTC)
