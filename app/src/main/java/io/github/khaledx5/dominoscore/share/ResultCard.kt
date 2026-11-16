package io.github.khaledx5.dominoscore.share

import androidx.compose.runtime.Immutable

enum class CardTitle {
    KING,
    KOOZ,
}

/** One line of the final ranking on the image: [mark] is 👑, 🪣 or the place number. */
@Immutable
data class ResultCardRow(
    val mark: String,
    val name: String,
    val score: String,
    val title: CardTitle?,
    val titleLabel: String?,
)

/**
 * Everything printed on the shared image, already in the app language. The screen builds it from
 * string resources, so the renderer only draws text and never decides wording.
 */
@Immutable
data class ResultCard(
    val brand: String,
    val trophy: String,
    val winnerLine: String,
    val scoreLine: String,
    val rows: List<ResultCardRow>,
    val footer: String,
    /** True for Arabic: the rows are mirrored so the mark sits on the right. */
    val rtl: Boolean,
)
