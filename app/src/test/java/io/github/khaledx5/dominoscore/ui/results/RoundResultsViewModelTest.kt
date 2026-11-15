package io.github.khaledx5.dominoscore.ui.results

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory
import io.github.khaledx5.dominoscore.domain.messages.MessageLibrary
import io.github.khaledx5.dominoscore.domain.messages.MessageTrigger
import io.github.khaledx5.dominoscore.domain.messages.PlayerMessageRecord
import io.github.khaledx5.dominoscore.domain.messages.RankingMessage
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.testing.FakePlayerMessageRepository
import io.github.khaledx5.dominoscore.domain.usecase.EndRoundUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObservePlayerMessagesUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.testClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoundResultsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val startGame = StartGameUseCase(games, testClock)
    private val recordScore = RecordScoreUseCase(games, testClock)
    private val endRound = EndRoundUseCase(games, testClock)
    private val messages = FakePlayerMessageRepository()
    private val library = MessageLibrary(
        listOf(RankingMessage("still_king_001", MessageCategory.STILL_KING, "التاج مش بيتساب بسهولة 👑")),
    )

    private fun viewModel(roundNumber: Int) = RoundResultsViewModel(
        SavedStateHandle(mapOf(RoundResultsViewModel.ROUND_NUMBER_ARG to roundNumber)),
        ObserveActiveGameUseCase(games),
        ObservePlayerMessagesUseCase(messages, { library }),
    )

    @Test
    fun showsTheCumulativeStandingsAfterTheRound() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona", "Ali"))
        val (omar, mona, _) = games.activeGame().players
        recordScore(mona.id, ScoreOperationType.ADD, 30)
        recordScore(omar.id, ScoreOperationType.PENALTY, 10)
        endRound()

        viewModel(roundNumber = 1).uiState.test {
            val results = expectMostRecentItem() as RoundResultsUiState.Ready
            assertEquals(1, results.roundNumber)
            assertEquals(2, results.nextRound)
            assertEquals(listOf("Mona", "Ali", "Omar"), results.standings.map { it.name })
            assertEquals(listOf(30, 0, -10), results.standings.map { it.score })
            assertTrue(results.standings.first().isKing)
            assertTrue(results.standings.last().isKooz)
        }
    }

    @Test
    fun closesWhenThereIsNoActiveGame() = runTest {
        viewModel(roundNumber = 1).uiState.test {
            assertEquals(RoundResultsUiState.NoActiveGame, expectMostRecentItem())
        }
    }

    @Test
    fun eachCardCarriesThePlayersLatestLine() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val game = games.activeGame()
        recordScore(game.players[0].id, ScoreOperationType.ADD, 30)
        messages.save(
            listOf(
                PlayerMessageRecord(
                    gameId = game.id,
                    playerId = game.players[0].id,
                    roundId = game.currentRound!!.id,
                    messageId = "still_king_001",
                    category = MessageCategory.STILL_KING,
                    trigger = MessageTrigger.ROUND_END,
                    createdAt = testClock.instant(),
                ),
            ),
        )
        endRound()

        viewModel(roundNumber = 1).uiState.test {
            val standings = (expectMostRecentItem() as RoundResultsUiState.Ready).standings
            assertEquals("التاج مش بيتساب بسهولة 👑", standings.first { it.name == "Omar" }.message?.text)
            assertEquals(null, standings.first { it.name == "Mona" }.message)
        }
    }
}
