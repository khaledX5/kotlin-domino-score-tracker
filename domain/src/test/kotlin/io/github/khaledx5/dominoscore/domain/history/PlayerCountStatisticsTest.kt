package io.github.khaledx5.dominoscore.domain.history

import app.cash.turbine.test
import io.github.khaledx5.dominoscore.domain.T0
import io.github.khaledx5.dominoscore.domain.fixedClock
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.Profile
import io.github.khaledx5.dominoscore.domain.model.ProfileId
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType.ADD
import io.github.khaledx5.dominoscore.domain.name
import io.github.khaledx5.dominoscore.domain.target
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.ConfirmWinnerUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerCountStatisticsTest {

    private val omar = profile(1, "Omar")
    private val mona = profile(2, "Mona")
    private val ali = profile(3, "Ali")
    private val sara = profile(4, "Sara")
    private var nextId = 1L

    private fun profile(id: Long, name: String) = Profile(ProfileId(id), name(name))

    /** A completed game: [results] are (person, final score) best first; the first one won unless [winner] says. */
    private fun game(vararg results: Pair<Profile, Int>, winner: Profile = results.first().first, rounds: Int = 3) =
        CompletedGame(
            id = GameId(nextId++),
            target = target(100),
            startedAt = T0,
            completedAt = T0.plusSeconds(1_800),
            roundCount = rounds,
            winnerId = PlayerId(nextId + results.indexOfFirst { it.first == winner }),
            standings = results.mapIndexed { index, (person, score) ->
                FinalStanding(
                    playerId = PlayerId(nextId + index),
                    profile = person,
                    name = person.name,
                    score = score,
                    rank = index + 1,
                    isKing = index == 0,
                    isKooz = index == results.lastIndex,
                )
            },
        ).also { nextId += results.size }

    private fun PlayerCountHistory.row(person: Profile) = leaderboard.single { it.statistics.profile == person }

    @Test
    fun keepsEveryTableSizeApart() {
        val games = listOf(
            game(omar to 120, mona to 40),
            game(omar to 110, mona to 20, ali to 10),
            game(ali to 130, sara to 90, omar to 30, mona to 0),
        )

        val two = historyFor(2, games)
        val three = historyFor(3, games)

        assertEquals(1, two.totalGames)
        assertEquals(2, two.totalPlayers)
        assertEquals(listOf(omar, mona), two.leaderboard.map { it.statistics.profile })
        assertEquals(1, three.totalGames)
        assertEquals(3, three.totalPlayers)
        assertEquals(1, three.row(omar).statistics.games)
        assertEquals(listOf(ali, sara, omar, mona), historyFor(4, games).leaderboard.map { it.statistics.profile })
    }

    @Test
    fun anEmptyTableSizeHasNoGamesAndNoLeaderboard() {
        val history = historyFor(4, listOf(game(omar to 120, mona to 40)))

        assertEquals(0, history.totalGames)
        assertEquals(0, history.totalPlayers)
        assertEquals(emptyList<LeaderboardEntry>(), history.leaderboard)
    }

    @Test
    fun countsGamesWinsTitlesAndAverages() {
        val history = historyFor(
            2,
            listOf(
                game(omar to 120, mona to 40),
                game(mona to 105, omar to 90),
                game(omar to 101, mona to -15),
            ),
        )

        val row = history.row(omar).statistics
        assertEquals(3, row.games)
        assertEquals(2, row.wins)
        assertEquals(67, row.winRatePercent)
        assertEquals(BigDecimal("103.7"), row.averageScore)
        assertEquals(2, row.kingFinishes)
        assertEquals(1, row.koozFinishes)
        assertEquals(BigDecimal("43.3"), history.row(mona).statistics.averageScore)
    }

    @Test
    fun winsAreTheConfirmedWinnerNotTheHighestScore() {
        // Both qualified and the table confirmed Mona although Omar finished higher.
        val history = historyFor(2, listOf(game(omar to 130, mona to 110, winner = mona)))

        assertEquals(0, history.row(omar).statistics.wins)
        assertEquals(1, history.row(mona).statistics.wins)
        assertEquals(1, history.row(omar).statistics.kingFinishes)
    }

    @Test
    fun winRateRoundsHalfUp() {
        val games = listOf(
            game(omar to 100, mona to 0),
            game(mona to 100, omar to 0),
            game(mona to 100, omar to 0),
            game(mona to 100, omar to 0),
            game(mona to 100, omar to 0),
            game(mona to 100, omar to 0),
            game(mona to 100, omar to 0),
            game(mona to 100, omar to 0),
        )

        // 1 / 8 = 12.5% and 7 / 8 = 87.5%.
        val history = historyFor(2, games)
        assertEquals(13, history.row(omar).statistics.winRatePercent)
        assertEquals(88, history.row(mona).statistics.winRatePercent)
    }

    @Test
    fun ordersByWinsThenWinRateThenAverageThenGames() {
        val hana = profile(5, "Hana")
        val history = historyFor(
            2,
            listOf(
                game(mona to 100, hana to 0),
                game(mona to 100, hana to 0),
                game(omar to 100, hana to 0),
                game(omar to 100, hana to 0),
                game(hana to 100, omar to 0),
                game(ali to 150, hana to 0),
                game(sara to 120, hana to 0),
            ),
        )

        // Mona 2/2 beats Omar 2/3 on win rate. Ali and Sara (1/1) tie on wins and rate: Ali has the
        // higher average. Hana also has 1 win, but in 7 games.
        assertEquals(listOf(mona, omar, ali, sara, hana), history.leaderboard.map { it.statistics.profile })
        assertEquals(listOf(1, 2, 3, 4, 5), history.leaderboard.map { it.position })
    }

    @Test
    fun equalRecordsFallBackToTheNameThenTheProfileId() {
        val otherOmar = profile(9, "omar")
        val history = historyFor(
            2,
            listOf(game(sara to 100, omar to 50), game(ali to 100, otherOmar to 50), game(mona to 100, omar to 50)),
        )

        val order = history.leaderboard.map { it.statistics.profile }
        // Ali and Sara: 1 win in 1, average 100, by name. Mona: same, after Ali by name.
        assertEquals(listOf(ali, mona, sara), order.take(3))
        // Omar has 2 games, the other "omar" 1: more games first, then the id would decide.
        assertEquals(listOf(omar, otherOmar), order.drop(3))
    }

    @Test
    fun theFirstThreePlacesCarryMedals() {
        val history = historyFor(
            4,
            listOf(game(omar to 120, mona to 90, ali to 60, sara to 0)),
        )

        assertEquals(
            listOf(Medal.GOLD, Medal.SILVER, Medal.BRONZE, null),
            history.leaderboard.map { it.medal },
        )
        assertNull(history.leaderboard.last().medal)
    }

    @Test
    fun theUseCaseFollowsEveryNewCompletedGame() = runTest {
        val repository = FakeGameRepository()
        val startGame = StartGameUseCase(repository, fixedClock)
        val recordScore = RecordScoreUseCase(repository, fixedClock)
        val confirmWinner = ConfirmWinnerUseCase(repository, fixedClock)

        ObservePlayerCountHistoryUseCase(repository)(playerCount = 2).test {
            assertEquals(0, awaitItem().totalGames)

            startGame(50, listOf("Omar", "Mona"))
            recordScore(repository.activeGame().players[1].id, ADD, 60)
            confirmWinner(repository.activeGame().players[1].id)

            val history = awaitItem()
            assertEquals(1, history.totalGames)
            assertEquals(listOf("Mona", "Omar"), history.leaderboard.map { it.statistics.profile.name.value })
            assertEquals(listOf(1, 0), history.leaderboard.map { it.statistics.wins })

            startGame(50, listOf("mona", "Ali", "Sara"))
            recordScore(repository.activeGame().players[0].id, ADD, 60)
            confirmWinner(repository.activeGame().players[0].id)
            expectNoEvents()
        }
    }
}
