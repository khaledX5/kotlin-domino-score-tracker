package io.github.khaledx5.dominoscore.domain.messages

import io.github.khaledx5.dominoscore.domain.model.Outcome
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.repository.PlayerMessageRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Smart Trash Talk: wraps a game command (a score, a target change, ending a round) from the
 * outside, so the use cases and their rules stay exactly as they are. It reads the table before,
 * runs the command, reads the table after, and saves one fresh message for each player the change
 * speaks to. It runs once per command, so repeated StateFlow emissions or recompositions can never
 * produce a message. A failed command, a missing game or an empty library simply mean no message.
 */
class RankingMessageEngine @Inject constructor(
    private val games: GameRepository,
    private val messages: PlayerMessageRepository,
    private val librarySource: MessageLibrarySource,
    private val selector: RankingMessageSelector,
    private val clock: Clock,
) {

    suspend fun <T, E> react(
        trigger: MessageTrigger,
        actor: PlayerId? = null,
        command: suspend () -> Outcome<T, E>,
    ): Outcome<T, E> {
        val before = games.findActiveGame() ?: return command()
        val round = before.currentRound ?: return command()
        val log = games.findScoreLog(before.id)

        val result = command()
        if (result is Outcome.Failure) return result
        val after = games.findActiveGame()?.takeIf { it.id == before.id } ?: return result

        // A round end looks back at the whole round; a tap or a target change only at that one change.
        val timeline = TitleTimeline(before.players, log)
        val roundEnd = trigger == MessageTrigger.ROUND_END
        val change = TableChange(
            before = if (roundEnd) timeline.lastRoundEnd else before.players,
            after = after.players,
            targetBefore = before.target,
            targetAfter = after.target,
            trigger = trigger,
            actor = actor,
        )
        val history = if (roundEnd) timeline.untilLastRoundEnd else timeline.untilNow
        val events = RankingEventDetector.detect(change, history)
        if (events.isEmpty()) return result

        val library = librarySource.load()
        val pastByPlayer = messages.history(before.id).groupBy { it.playerId }
        val takenNow = mutableSetOf<String>()
        val now = clock.instant()
        val records = events.mapNotNull { event ->
            val message = selector.select(event.category, library, pastByPlayer[event.playerId].orEmpty(), takenNow)
                ?: return@mapNotNull null
            takenNow += message.id
            PlayerMessageRecord(
                gameId = before.id,
                playerId = event.playerId,
                roundId = round.id,
                messageId = message.id,
                category = message.category,
                trigger = trigger,
                createdAt = now,
            )
        }
        messages.save(records)
        return result
    }
}
