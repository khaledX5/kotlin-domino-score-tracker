package io.github.khaledx5.dominoscore.domain.messages

import io.github.khaledx5.dominoscore.domain.T0
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.GENERAL_HUMOR
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.STILL_KING
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.STILL_KOOZ
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.TIED_FIRST
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.RoundId
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingMessageSelectorTest {

    private val selector = RankingMessageSelector(Random(seed = 7))

    private fun lines(category: MessageCategory, count: Int, enabled: Boolean = true) = (1..count).map {
        RankingMessage(
            id = "${category.name.lowercase()}_$it",
            category = category,
            text = "line $it",
            enabled = enabled,
        )
    }

    private fun record(messageId: String, category: MessageCategory = STILL_KING, player: Long = 1) =
        PlayerMessageRecord(
            gameId = GameId(1),
            playerId = PlayerId(player),
            roundId = RoundId(1),
            messageId = messageId,
            category = category,
            trigger = MessageTrigger.ROUND_END,
            createdAt = T0,
        )

    @Test
    fun picksFromTheAskedCategory() {
        val library = MessageLibrary(lines(STILL_KING, 4) + lines(STILL_KOOZ, 4))

        assertEquals(STILL_KING, selector.select(STILL_KING, library, emptyList())?.category)
    }

    @Test
    fun neverTheSameMessageTwiceInARow() {
        val library = MessageLibrary(lines(STILL_KING, 2))
        val history = mutableListOf<PlayerMessageRecord>()

        repeat(50) {
            val picked = selector.select(STILL_KING, library, history)!!
            history.lastOrNull()?.let { assertNotEquals(it.messageId, picked.id) }
            history += record(picked.id)
        }
    }

    @Test
    fun avoidsTheLastThreeInTheCategoryWhileThereIsChoice() {
        val library = MessageLibrary(lines(STILL_KING, 5))
        val history = mutableListOf<PlayerMessageRecord>()

        repeat(60) {
            val picked = selector.select(STILL_KING, library, history)!!
            assertTrue(picked.id !in history.takeLast(3).map { it.messageId })
            history += record(picked.id)
        }
    }

    @Test
    fun whenEverythingWasUsedRecentLinesComeBackButNotTheLastOne() {
        val library = MessageLibrary(lines(STILL_KING, 3))
        val history = listOf(record("still_king_1"), record("still_king_2"), record("still_king_3"))

        val picked = selector.select(STILL_KING, library, history)!!

        assertNotEquals("still_king_3", picked.id)
    }

    @Test
    fun aSingleLineCategoryStillAnswers() {
        val library = MessageLibrary(lines(TIED_FIRST, 1))

        assertEquals(
            "tied_first_1",
            selector.select(TIED_FIRST, library, listOf(record("tied_first_1", TIED_FIRST)))?.id,
        )
    }

    @Test
    fun playersInTheSameEventGetDifferentLinesWhenPossible() {
        val library = MessageLibrary(lines(STILL_KOOZ, 3))

        repeat(30) {
            val first = selector.select(STILL_KOOZ, library, emptyList())!!
            val second = selector.select(STILL_KOOZ, library, emptyList(), takenNow = setOf(first.id))!!
            assertNotEquals(first.id, second.id)
        }
    }

    @Test
    fun eachPlayerHasTheirOwnHistory() {
        val library = MessageLibrary(lines(STILL_KING, 2))
        val omarsLast = record("still_king_1", player = 1)

        // Mona's history is empty, so Omar's last line is still open to her.
        val picks = (1..40).map { selector.select(STILL_KING, library, history = emptyList())!!.id }.toSet()
        assertTrue(omarsLast.messageId in picks)
    }

    @Test
    fun disabledLinesAreNeverPicked() {
        val library = MessageLibrary(lines(STILL_KING, 3, enabled = false) + lines(GENERAL_HUMOR, 2))

        repeat(20) { assertEquals(GENERAL_HUMOR, selector.select(STILL_KING, library, emptyList())?.category) }
    }

    @Test
    fun anEmptyCategoryFallsBackToGeneralHumor() {
        val library = MessageLibrary(lines(GENERAL_HUMOR, 2))

        assertEquals(GENERAL_HUMOR, selector.select(TIED_FIRST, library, emptyList())?.category)
    }

    @Test
    fun anEmptyLibraryGivesNoMessage() {
        assertNull(selector.select(STILL_KING, MessageLibrary.EMPTY, emptyList()))
    }
}
