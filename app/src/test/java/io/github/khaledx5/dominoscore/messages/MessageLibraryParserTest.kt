package io.github.khaledx5.dominoscore.messages

import io.github.khaledx5.dominoscore.domain.messages.MessageCategory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageLibraryParserTest {

    private fun library(categories: String, version: Int = 1) =
        """{ "version": $version, "language": "ar-EG", "categories": [ $categories ] }"""

    private fun category(id: String, vararg messages: String) =
        """{ "id": "$id", "messages": [ ${messages.joinToString()} ] }"""

    private fun message(id: String, text: String = "نص $id 😂", enabled: Boolean = true) =
        """{ "id": "$id", "text": "$text", "emoji": "😂", "enabled": $enabled }"""

    @Test
    fun readsMessagesByCategory() {
        val parsed = MessageLibraryParser.parse(
            library(category("STILL_KING", message("still_king_001"), message("still_king_002"))),
        )

        assertEquals(emptyList<String>(), parsed.problems)
        assertEquals(
            listOf("still_king_001", "still_king_002"),
            parsed.library.enabled(MessageCategory.STILL_KING).map {
                it.id
            },
        )
    }

    @Test
    fun disabledMessagesAreKeptButNeverOffered() {
        val parsed = MessageLibraryParser.parse(
            library(category("STILL_KING", message("a"), message("b", enabled = false))),
        )

        assertEquals(listOf("a"), parsed.library.enabled(MessageCategory.STILL_KING).map { it.id })
        assertEquals("b", parsed.library.find("b")?.id)
    }

    @Test
    fun anEmptyCategoryIsFine() {
        val parsed = MessageLibraryParser.parse(library(category("TIED_FIRST")))

        assertEquals(emptyList<String>(), parsed.problems)
        assertTrue(parsed.library.enabled(MessageCategory.TIED_FIRST).isEmpty())
    }

    @Test
    fun badEntriesAreSkippedAndTheRestIsKept() {
        val parsed = MessageLibraryParser.parse(
            library(
                listOf(
                    category("STILL_KONG", message("x")),
                    category("STILL_KING", message("a"), message("a"), message(" "), message("c", text = " ")),
                    category("STILL_KOOZ", message("d")),
                ).joinToString(),
            ),
        )

        assertEquals(4, parsed.problems.size)
        assertEquals(listOf("a"), parsed.library.enabled(MessageCategory.STILL_KING).map { it.id })
        assertEquals(listOf("d"), parsed.library.enabled(MessageCategory.STILL_KOOZ).map { it.id })
    }

    @Test
    fun brokenJsonGivesAnEmptyLibrary() {
        val parsed = MessageLibraryParser.parse("{ not json")

        assertEquals(0, parsed.library.size)
        assertEquals(1, parsed.problems.size)
    }

    @Test
    fun aMissingRequiredFieldGivesAnEmptyLibrary() {
        val parsed = MessageLibraryParser.parse("""{ "version": 1 }""")

        assertEquals(0, parsed.library.size)
    }

    @Test
    fun anotherVersionGivesAnEmptyLibrary() {
        val parsed = MessageLibraryParser.parse(library(category("STILL_KING", message("a")), version = 2))

        assertEquals(0, parsed.library.size)
        assertTrue(parsed.problems.single().contains("version 2"))
    }

    @Test
    fun theShippedLibraryHasEveryApprovedMessage() {
        // Unit tests run from the module folder, so the real asset is read as it will be packaged.
        val parsed = MessageLibraryParser.parse(File("src/main/assets/ranking_messages.json").readText())

        assertEquals(emptyList<String>(), parsed.problems)
        assertEquals(111, parsed.library.size)
        MessageCategory.entries.forEach { category ->
            assertTrue("$category has messages", parsed.library.enabled(category).isNotEmpty())
        }
        assertEquals(15, parsed.library.enabled(MessageCategory.FIRST_TIME_KING).size)
        assertEquals(16, parsed.library.enabled(MessageCategory.FIRST_TIME_KOOZ).size)
        assertEquals("على الله حكايته 😂", parsed.library.find("first_time_kooz_001")?.text)
    }
}
