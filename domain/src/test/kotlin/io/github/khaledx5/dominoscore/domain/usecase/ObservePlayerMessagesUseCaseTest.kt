package io.github.khaledx5.dominoscore.domain.usecase

import app.cash.turbine.test
import io.github.khaledx5.dominoscore.domain.T0
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory
import io.github.khaledx5.dominoscore.domain.messages.MessageLibrary
import io.github.khaledx5.dominoscore.domain.messages.MessageTrigger
import io.github.khaledx5.dominoscore.domain.messages.PlayerMessageRecord
import io.github.khaledx5.dominoscore.domain.messages.RankingMessage
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.RoundId
import io.github.khaledx5.dominoscore.domain.testing.FakePlayerMessageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObservePlayerMessagesUseCaseTest {

    private val messages = FakePlayerMessageRepository()
    private val line = RankingMessage("still_kooz_001", MessageCategory.STILL_KOOZ, "الكوز مقيم معانا 😂")
    private val observe = ObservePlayerMessagesUseCase(messages) { MessageLibrary(listOf(line)) }

    private fun record(player: Long, messageId: String) = PlayerMessageRecord(
        gameId = GameId(1),
        playerId = PlayerId(player),
        roundId = RoundId(1),
        messageId = messageId,
        category = MessageCategory.STILL_KOOZ,
        trigger = MessageTrigger.ROUND_END,
        createdAt = T0,
    )

    @Test
    fun givesEachPlayerTheTextOfTheirLatestLine() = runTest {
        messages.save(listOf(record(player = 1, messageId = "still_kooz_001")))

        observe(GameId(1)).test {
            assertEquals(mapOf(PlayerId(1) to line), awaitItem())
        }
    }

    @Test
    fun aLineThatLeftTheLibraryIsSkippedQuietly() = runTest {
        messages.save(listOf(record(player = 2, messageId = "removed_line_009")))

        observe(GameId(1)).test {
            assertEquals(emptyMap<PlayerId, RankingMessage>(), awaitItem())
        }
    }
}
