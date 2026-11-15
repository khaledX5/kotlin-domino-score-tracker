package io.github.khaledx5.dominoscore.domain.messages

/** How strong a reason a message has. A stronger tier always wins over a weaker one for the same player. */
enum class MessageTier {
    /** Something rare happened on the table: last to first, one point short, a big comeback... */
    SPECIAL,

    /** The player just took or fell into a title. */
    TRANSITION,

    /** The player kept their title for a whole round. */
    STILL,

    /** Nothing special: a light joke at the end of a round. */
    GENERAL,
}

/** Which title a message talks about, so the screen can colour it: gold for KING, coral for KOOZ. */
enum class MessageTone {
    KING,
    KOOZ,
    NEUTRAL,
}

/**
 * Every kind of message the table can get. The names are the category ids of the message library,
 * so they must never be renamed. Special events are listed from the highest priority down:
 * when several apply to one player at once, the first one in this order is shown.
 */
enum class MessageCategory(val tier: MessageTier, val tone: MessageTone) {
    LAST_TO_FIRST(MessageTier.SPECIAL, MessageTone.KING),
    FIRST_TO_LAST(MessageTier.SPECIAL, MessageTone.KOOZ),
    MAJOR_COMEBACK(MessageTier.SPECIAL, MessageTone.KING),
    LOST_LONG_STANDING_FIRST(MessageTier.SPECIAL, MessageTone.NEUTRAL),
    ONE_POINT_LOST_FIRST(MessageTier.SPECIAL, MessageTone.NEUTRAL),
    EXCEEDED_TARGET(MessageTier.SPECIAL, MessageTone.NEUTRAL),
    REACHED_TARGET(MessageTier.SPECIAL, MessageTone.NEUTRAL),
    ESCAPED_KOOZ(MessageTier.SPECIAL, MessageTone.NEUTRAL),
    NEGATIVE_TO_POSITIVE(MessageTier.SPECIAL, MessageTone.NEUTRAL),
    GAINED_THREE_RANKS(MessageTier.SPECIAL, MessageTone.NEUTRAL),
    LOST_TWO_RANKS(MessageTier.SPECIAL, MessageTone.NEUTRAL),
    TIED_FIRST(MessageTier.SPECIAL, MessageTone.KING),
    OVERTAKEN_PLAYER(MessageTier.SPECIAL, MessageTone.NEUTRAL),
    EXTREMELY_NEGATIVE_SCORE(MessageTier.SPECIAL, MessageTone.NEUTRAL),
    ALL_PLAYERS_NEGATIVE(MessageTier.SPECIAL, MessageTone.NEUTRAL),

    FIRST_TIME_KING(MessageTier.TRANSITION, MessageTone.KING),
    KING_COMEBACK(MessageTier.TRANSITION, MessageTone.KING),
    FIRST_TIME_KOOZ(MessageTier.TRANSITION, MessageTone.KOOZ),
    KOOZ_COMEBACK(MessageTier.TRANSITION, MessageTone.KOOZ),

    STILL_KING(MessageTier.STILL, MessageTone.KING),
    STILL_KOOZ(MessageTier.STILL, MessageTone.KOOZ),

    GENERAL_HUMOR(MessageTier.GENERAL, MessageTone.NEUTRAL),
    ;

    companion object {
        /** The category with the library id [id], or null for an id this version does not know. */
        fun fromId(id: String): MessageCategory? = entries.firstOrNull { it.name == id }
    }
}

/** Highest priority first: by tier, then by the order of the special events above. */
val messagePriority: Comparator<MessageCategory> = compareBy<MessageCategory> { it.tier }.thenBy { it.ordinal }

/** The one category to show when several apply to the same player, or null when none does. */
fun Collection<MessageCategory>.strongest(): MessageCategory? = minWithOrNull(messagePriority)

/** What happened on the table. Score taps and target changes react to changes only; a round end speaks to everyone. */
enum class MessageTrigger {
    SCORE_CHANGE,
    TARGET_CHANGE,
    ROUND_END,
}
