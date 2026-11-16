package io.github.khaledx5.dominoscore.domain.messages

import io.github.khaledx5.dominoscore.domain.command.GameCommand
import io.github.khaledx5.dominoscore.domain.command.GameTransition
import io.github.khaledx5.dominoscore.domain.command.GameTransitionListener
import io.github.khaledx5.dominoscore.domain.repository.PlayerMessageRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Smart Trash Talk: listens to every game command (a score, a target change, ending a round) and
 * saves one fresh message for each player the change speaks to. The command runner calls it once
 * per command, so repeated StateFlow emissions or recompositions can never produce a message.
 * A refused command or an empty library simply mean no message.
 */
class RankingMessageEngine @Inject constructor(
    private val messages: PlayerMessageRepository,
    private val librarySource: MessageLibrarySource,
    private val selector: RankingMessageSelector,
    private val clock: Clock,
) : GameTransitionListener {

    override suspend fun onTransition(transition: GameTransition) {
        val (command, before, log, after) = transition
        val round = before.currentRound ?: return
        val trigger = command.trigger
        val actor = (command as? GameCommand.Score)?.playerId

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
        if (events.isEmpty()) return

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
    }

    private val GameCommand.trigger: MessageTrigger
        get() = when (this) {
            is GameCommand.Score -> MessageTrigger.SCORE_CHANGE
            GameCommand.ChangeTarget -> MessageTrigger.TARGET_CHANGE
            GameCommand.EndRound -> MessageTrigger.ROUND_END
        }
}
