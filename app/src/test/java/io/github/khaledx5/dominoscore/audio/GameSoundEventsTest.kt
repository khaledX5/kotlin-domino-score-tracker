package io.github.khaledx5.dominoscore.audio

import io.github.khaledx5.dominoscore.domain.command.GameCommand
import io.github.khaledx5.dominoscore.domain.command.GameCommandRunner
import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEventDetector
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.FakeMusicSettingsStore
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.RecordingAudio
import io.github.khaledx5.dominoscore.testing.testClock
import kotlin.math.abs
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GameSoundEventsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val audio = RecordingAudio()
    private val store = FakeMusicSettingsStore()
    private val games = FakeGameRepository()
    private val music = MusicController(
        audio.manager,
        store,
        ObserveActiveGameUseCase(games),
        TestScope(mainDispatcherRule.dispatcher),
    )
    private val commands = GameCommandRunner(games, setOf(GameSoundEvents(GameSoundEventDetector(), music)))
    private val recordScore = RecordScoreUseCase(games, testClock)

    private suspend fun start(target: Int, vararg names: String): List<Player> {
        StartGameUseCase(games, testClock)(targetScore = target, playerNames = names.toList())
        return games.activeGame().players
    }

    private suspend fun score(player: Player, points: Int) {
        val type = if (points >= 0) ScoreOperationType.ADD else ScoreOperationType.PENALTY
        commands.run(GameCommand.Score(player.id, type, abs(points))) { recordScore(player.id, type, abs(points)) }
        audio.now += GameAudioManager.EVENT_COOLDOWN_MS
    }

    private fun events() = audio.started.filter { it.isEventSound }

    @Test
    fun aLargePenaltyPlaysItsSound() = runTest {
        val (omar, _) = start(100, "Omar", "Mona")
        score(omar, 20)

        score(omar, -5)

        assertEquals(listOf(MusicTrack.EVENT_LARGE_PENALTY), events())
    }

    @Test
    fun anOrdinaryScorePlaysNothing() = runTest {
        val (omar, mona, _) = start(100, "Omar", "Mona", "Ali")

        score(omar, 10)
        score(omar, -2)
        // Ali is still on 0, so Mona is not the last one to score.
        score(mona, 5)

        assertEquals(emptyList<MusicTrack>(), events())
    }

    @Test
    fun oneCommandPlaysOnlyItsHighestEvent() = runTest {
        val (omar, mona) = start(100, "Omar", "Mona")
        score(omar, 100)
        score(mona, 90)
        audio.started.clear()

        // Mona reaches 100 too: a second qualified player and a tie on 100, but only one sound.
        score(mona, 10)

        assertEquals(listOf(MusicTrack.EVENT_FINAL_STRETCH), events())
    }

    @Test
    fun theSameTableNeverPlaysTwice() = runTest {
        val (omar, mona, ali) = start(100, "Omar", "Mona", "Ali")
        score(omar, 30)
        score(mona, 30)
        score(ali, 3)
        audio.started.clear()

        // The tie on 30 and Ali leaving 0 already played; the same table plays nothing more.
        score(ali, 3)
        score(ali, 3)

        assertEquals(emptyList<MusicTrack>(), events())
    }

    @Test
    fun aSwitchedOffEventStaysSilentWhileOthersStillPlay() = runTest {
        store.setEventSoundEnabled(GameSoundEvent.LARGE_PENALTY, false)
        val (omar, mona, ali) = start(100, "Omar", "Mona", "Ali")
        score(omar, 30)

        score(omar, -5)
        score(mona, 26)
        score(ali, 1)
        score(ali, 25)

        assertEquals(listOf(MusicTrack.EVENT_LAST_PLAYER_SCORES, MusicTrack.EVENT_EQUAL_SCORES), events())
    }

    @Test
    fun theMasterSwitchSilencesEveryEvent() = runTest {
        store.setEventSoundsEnabled(false)
        val (omar, _) = start(100, "Omar", "Mona")
        score(omar, 30)

        score(omar, -10)

        assertEquals(emptyList<MusicTrack>(), events())
    }

    @Test
    fun everyEventHasAnEventClip() {
        GameSoundEvent.entries.forEach { assertEquals(true, it.track.isEventSound) }
    }
}
