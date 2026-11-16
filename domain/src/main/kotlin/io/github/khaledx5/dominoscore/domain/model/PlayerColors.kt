package io.github.khaledx5.dominoscore.domain.model

/**
 * Players are told apart by a color the table picks at setup. The domain only knows the color's
 * index in the palette; the app decides what each index looks like.
 */
object PlayerColors {
    const val COUNT = 8

    val INDICES: IntRange = 0 until COUNT

    /** Seat order: the colors a game gets when nobody picks any. */
    fun defaultFor(playerCount: Int): List<Int> = (0 until playerCount).toList()

    /** One valid color per seat, and no color used twice in the same game. */
    fun areValid(colorIndexes: List<Int>): Boolean =
        colorIndexes.all { it in INDICES } && colorIndexes.toSet().size == colorIndexes.size
}
