package io.github.khaledx5.dominoscore.domain.messages

import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.RoundId
import java.time.Instant
import javax.inject.Inject
import kotlin.random.Random

/** One message a player got. Keyed by player id, never by name, so a rename keeps the history. */
data class PlayerMessageRecord(
    val gameId: GameId,
    val playerId: PlayerId,
    val roundId: RoundId,
    val messageId: String,
    val category: MessageCategory,
    val trigger: MessageTrigger,
    val createdAt: Instant,
)

/**
 * Picks a line from the library at random, without the repeats that make a joke go stale:
 * never the player's previous message, preferably none of their last [RECENT_PER_CATEGORY] in the
 * category, and preferably not one another player gets in the same event. When the category runs
 * out, those preferences are dropped one by one, so older lines come back. An empty category falls
 * back to GENERAL_HUMOR, and an empty library gives no message at all.
 */
class RankingMessageSelector @Inject constructor(private val random: Random) {

    /**
     * @param history this player's messages in this game, oldest first.
     * @param takenNow message ids already given to other players in the same event.
     */
    fun select(
        category: MessageCategory,
        library: MessageLibrary,
        history: List<PlayerMessageRecord>,
        takenNow: Set<String> = emptySet(),
    ): RankingMessage? {
        val candidates = library.enabled(category).ifEmpty { library.enabled(MessageCategory.GENERAL_HUMOR) }
        if (candidates.isEmpty()) return null
        val chosenCategory = candidates.first().category

        val previous = history.lastOrNull()?.messageId
        val recent = history.filter { it.category == chosenCategory }
            .takeLast(RECENT_PER_CATEGORY)
            .map { it.messageId }
            .toSet()

        val rounds = sequenceOf<(RankingMessage) -> Boolean>(
            { it.id != previous && it.id !in recent && it.id !in takenNow },
            { it.id != previous && it.id !in recent },
            { it.id != previous && it.id !in takenNow },
            { it.id != previous },
            { true },
        )
        val pool = rounds.map { rule -> candidates.filter(rule) }.first { it.isNotEmpty() }
        return pool.random(random)
    }

    companion object {
        const val RECENT_PER_CATEGORY = 3
    }
}
