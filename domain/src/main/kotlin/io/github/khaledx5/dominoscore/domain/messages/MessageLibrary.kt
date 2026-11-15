package io.github.khaledx5.dominoscore.domain.messages

/** One approved line of the library, shown exactly as written. */
data class RankingMessage(val id: String, val category: MessageCategory, val text: String, val enabled: Boolean = true)

/**
 * The approved messages, grouped by category. Built from a file the app ships, so it can grow to
 * hundreds of lines without touching code. An empty library is valid: the game simply stays quiet.
 */
class MessageLibrary(messages: List<RankingMessage>) {

    private val byId: Map<String, RankingMessage> = messages.associateBy { it.id }
    private val enabledByCategory: Map<MessageCategory, List<RankingMessage>> =
        messages.filter { it.enabled }.groupBy { it.category }

    val size: Int get() = byId.size

    fun enabled(category: MessageCategory): List<RankingMessage> = enabledByCategory[category].orEmpty()

    /** A saved message id may come from an older library: null means the line is gone. */
    fun find(id: String): RankingMessage? = byId[id]

    companion object {
        val EMPTY = MessageLibrary(emptyList())
    }
}

/** Where the library comes from. Loading never throws: a broken or missing source gives [MessageLibrary.EMPTY]. */
fun interface MessageLibrarySource {
    suspend fun load(): MessageLibrary
}
