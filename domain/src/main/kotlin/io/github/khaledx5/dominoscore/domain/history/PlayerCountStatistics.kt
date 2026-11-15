package io.github.khaledx5.dominoscore.domain.history

import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.Profile
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** What one person did in the completed games of one table size. */
data class PlayerStatistics(
    val profile: Profile,
    val games: Int,
    /** Official wins only: the winner the table confirmed, never guessed from the scores. */
    val wins: Int,
    val kingFinishes: Int,
    val koozFinishes: Int,
    private val totalScore: Long,
) {
    /** wins / games x 100, rounded half up to a whole percent. */
    val winRatePercent: Int get() = ((wins * 200L + games) / (games * 2L)).toInt()

    /** The average final score, rounded half up to one decimal. */
    val averageScore: BigDecimal
        get() = BigDecimal.valueOf(totalScore).divide(BigDecimal.valueOf(games.toLong()), 1, RoundingMode.HALF_UP)

    internal fun compareRateTo(other: PlayerStatistics): Int =
        (wins.toLong() * other.games).compareTo(other.wins.toLong() * games)

    internal fun compareAverageTo(other: PlayerStatistics): Int =
        (BigDecimal.valueOf(totalScore) * BigDecimal.valueOf(other.games.toLong()))
            .compareTo(BigDecimal.valueOf(other.totalScore) * BigDecimal.valueOf(games.toLong()))
}

enum class Medal {
    GOLD,
    SILVER,
    BRONZE,
}

/** A row of the leaderboard: [position] counts from 1 and the first three carry a medal. */
data class LeaderboardEntry(val position: Int, val statistics: PlayerStatistics) {
    val medal: Medal? get() = Medal.entries.getOrNull(position - 1)
}

/** History and statistics of one table size. Nothing from games of another size is mixed in. */
data class PlayerCountHistory(
    val playerCount: Int,
    /** Newest first. */
    val games: List<CompletedGame>,
    val leaderboard: List<LeaderboardEntry>,
) {
    val totalGames: Int get() = games.size

    val totalPlayers: Int get() = leaderboard.size
}

/**
 * Best first: more wins, then a higher win rate, a higher average final score, more games, then
 * the name and the profile id, so equal records always come out in the same order. Rates and
 * averages are compared exactly, not as the rounded numbers on screen.
 */
private val leaderboardOrder: Comparator<PlayerStatistics> =
    compareByDescending<PlayerStatistics> { it.wins }
        .then { a, b -> b.compareRateTo(a) }
        .then { a, b -> b.compareAverageTo(a) }
        .thenByDescending { it.games }
        .thenBy { it.profile.name.key }
        .thenBy { it.profile.id.value }

/** Builds the history and leaderboard of the games with exactly [playerCount] players. */
fun historyFor(playerCount: Int, completedGames: List<CompletedGame>): PlayerCountHistory {
    require(playerCount in Game.PLAYER_COUNT) { "Unsupported player count $playerCount" }
    val games = completedGames.filter { it.playerCount == playerCount }
    val statistics = games
        .flatMap { game -> game.standings.map { standing -> game to standing } }
        .groupBy { (_, standing) -> standing.profile.id }
        .values
        .map { results ->
            PlayerStatistics(
                profile = results.first().second.profile,
                games = results.size,
                wins = results.count { (game, standing) -> game.winnerId == standing.playerId },
                kingFinishes = results.count { (_, standing) -> standing.isKing },
                koozFinishes = results.count { (_, standing) -> standing.isKooz },
                totalScore = results.sumOf { (_, standing) -> standing.score.toLong() },
            )
        }
        .sortedWith(leaderboardOrder)
    return PlayerCountHistory(
        playerCount = playerCount,
        games = games,
        leaderboard = statistics.mapIndexed { index, it -> LeaderboardEntry(position = index + 1, statistics = it) },
    )
}

class ObservePlayerCountHistoryUseCase @Inject constructor(private val repository: GameRepository) {
    /** The completed games with exactly [playerCount] players and their leaderboard, again after every change. */
    operator fun invoke(playerCount: Int): Flow<PlayerCountHistory> = repository.observeCompletedGames()
        .map { games -> historyFor(playerCount, games) }
        .distinctUntilChanged()
}
