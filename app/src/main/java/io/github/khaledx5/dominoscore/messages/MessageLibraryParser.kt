package io.github.khaledx5.dominoscore.messages

import io.github.khaledx5.dominoscore.domain.messages.MessageCategory
import io.github.khaledx5.dominoscore.domain.messages.MessageLibrary
import io.github.khaledx5.dominoscore.domain.messages.RankingMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** The library as read from JSON, before any check. */
@Serializable
private data class LibraryFile(val version: Int, val language: String = "", val categories: List<CategoryEntry>)

@Serializable
private data class CategoryEntry(val id: String, val messages: List<MessageEntry> = emptyList())

@Serializable
private data class MessageEntry(val id: String, val text: String, val emoji: String = "", val enabled: Boolean = true)

/** The checked library plus what was wrong with the file, for the log. */
data class ParsedLibrary(val library: MessageLibrary, val problems: List<String>)

/**
 * Reads and checks ranking_messages.json. A file that cannot be read, or has another version,
 * gives an empty library. Single bad entries (unknown category, blank or duplicate id, blank text)
 * are skipped and reported, and the rest of the library is kept.
 */
object MessageLibraryParser {

    const val SUPPORTED_VERSION = 1

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(content: String): ParsedLibrary {
        val file = try {
            json.decodeFromString<LibraryFile>(content)
        } catch (error: SerializationException) {
            return empty("Unreadable message library: ${error.message}")
        } catch (error: IllegalArgumentException) {
            return empty("Invalid message library: ${error.message}")
        }
        if (file.version != SUPPORTED_VERSION) {
            return empty("Unsupported message library version ${file.version}")
        }

        val problems = mutableListOf<String>()
        val seenIds = mutableSetOf<String>()
        val messages = file.categories.flatMap { entry ->
            val category = MessageCategory.fromId(entry.id)
            if (category == null) {
                problems += "Unknown category ${entry.id}: ${entry.messages.size} messages skipped"
                return@flatMap emptyList()
            }
            entry.messages.mapNotNull { message ->
                when {
                    message.id.isBlank() -> null.also { problems += "A message in ${entry.id} has no id" }
                    !seenIds.add(message.id) -> null.also { problems += "Duplicate message id ${message.id}" }
                    message.text.isBlank() -> null.also { problems += "Message ${message.id} has no text" }
                    else -> RankingMessage(message.id, category, message.text, message.enabled)
                }
            }
        }
        return ParsedLibrary(MessageLibrary(messages), problems)
    }

    private fun empty(problem: String) = ParsedLibrary(MessageLibrary.EMPTY, listOf(problem))
}
