package io.github.khaledx5.dominoscore.domain.rules

import io.github.khaledx5.dominoscore.domain.model.Game

/** A game together with everything derived from it, computed in one place for the game screen. */
data class GameSnapshot(
    val game: Game,
    val ranking: List<RankedPlayer>,
    val primaryAction: PrimaryAction,
    val winnerSelection: WinnerSelection,
) {
    companion object {
        fun of(game: Game): GameSnapshot = GameSnapshot(
            game = game,
            ranking = rankPlayers(game.players),
            primaryAction = primaryActionFor(game.players, game.target),
            winnerSelection = selectWinner(game.players, game.target),
        )
    }
}
