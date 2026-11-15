package io.github.khaledx5.dominoscore.domain.messages

import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.ALL_PLAYERS_NEGATIVE
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.ESCAPED_KOOZ
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.FIRST_TIME_KING
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.GENERAL_HUMOR
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.LAST_TO_FIRST
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.NEGATIVE_TO_POSITIVE
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.ONE_POINT_LOST_FIRST
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.STILL_KING
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory.TIED_FIRST
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageCategoryTest {

    @Test
    fun thereAreFifteenSpecialEventsAndOneGeneralCategory() {
        assertEquals(15, MessageCategory.entries.count { it.tier == MessageTier.SPECIAL })
        assertEquals(listOf(GENERAL_HUMOR), MessageCategory.entries.filter { it.tier == MessageTier.GENERAL })
        assertEquals(22, MessageCategory.entries.size)
    }

    @Test
    fun aSpecialEventBeatsATitleTransition() {
        assertEquals(ESCAPED_KOOZ, listOf(FIRST_TIME_KING, ESCAPED_KOOZ).strongest())
    }

    @Test
    fun aTitleTransitionBeatsStillHoldingIt() {
        assertEquals(FIRST_TIME_KING, listOf(STILL_KING, FIRST_TIME_KING).strongest())
    }

    @Test
    fun anythingBeatsGeneralHumor() {
        assertEquals(STILL_KING, listOf(GENERAL_HUMOR, STILL_KING).strongest())
    }

    @Test
    fun lastToFirstIsTheStrongestSpecialEvent() {
        assertEquals(LAST_TO_FIRST, listOf(TIED_FIRST, NEGATIVE_TO_POSITIVE, LAST_TO_FIRST).strongest())
    }

    @Test
    fun specialEventsFollowTheAgreedOrder() {
        assertEquals(ONE_POINT_LOST_FIRST, listOf(ALL_PLAYERS_NEGATIVE, ESCAPED_KOOZ, ONE_POINT_LOST_FIRST).strongest())
    }

    @Test
    fun noCategoriesMeansNoMessage() {
        assertNull(emptyList<MessageCategory>().strongest())
    }

    @Test
    fun categoriesAreFoundByTheirLibraryId() {
        assertEquals(STILL_KING, MessageCategory.fromId("STILL_KING"))
        assertNull(MessageCategory.fromId("STILL_KONG"))
    }
}
