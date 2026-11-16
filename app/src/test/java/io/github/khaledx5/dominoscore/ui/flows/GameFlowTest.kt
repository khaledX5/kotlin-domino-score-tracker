package io.github.khaledx5.dominoscore.ui.flows

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.khaledx5.dominoscore.audio.GameSoundEvents
import io.github.khaledx5.dominoscore.audio.MusicController
import io.github.khaledx5.dominoscore.domain.command.GameCommandRunner
import io.github.khaledx5.dominoscore.domain.history.ObserveMovesUseCase
import io.github.khaledx5.dominoscore.domain.messages.MessageLibrary
import io.github.khaledx5.dominoscore.domain.messages.RankingMessageEngine
import io.github.khaledx5.dominoscore.domain.messages.RankingMessageSelector
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEventDetector
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.testing.FakePlayerMessageRepository
import io.github.khaledx5.dominoscore.domain.usecase.ChangeTargetUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ConfirmWinnerUseCase
import io.github.khaledx5.dominoscore.domain.usecase.EndRoundUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObservePlayerMessagesUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RenamePlayerUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.FakeMusicSettingsStore
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.RecordingAudio
import io.github.khaledx5.dominoscore.testing.testClock
import io.github.khaledx5.dominoscore.ui.game.GameScreen
import io.github.khaledx5.dominoscore.ui.game.GameViewModel
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import kotlin.random.Random
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A whole game played through the game screen: scores, the end of a round and the winner. */
@RunWith(RobolectricTestRunner::class)
class GameFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val games = FakeGameRepository()
    private val messages = FakePlayerMessageRepository()
    private var roundShown: Int? = null
    private var celebrated: Long? = null

    private fun showGame() {
        val music = MusicController(
            audio = RecordingAudio().manager,
            settingsStore = FakeMusicSettingsStore(),
            observeActiveGame = ObserveActiveGameUseCase(games),
            scope = TestScope(mainDispatcherRule.dispatcher),
        )
        val soundEvents = GameSoundEvents(GameSoundEventDetector(), music)
        val viewModel = GameViewModel(
            ObserveActiveGameUseCase(games),
            ObservePlayerMessagesUseCase(messages) { MessageLibrary.EMPTY },
            ObserveMovesUseCase(games),
            RecordScoreUseCase(games, testClock),
            ChangeTargetUseCase(games, testClock),
            EndRoundUseCase(games, testClock),
            ConfirmWinnerUseCase(games, testClock),
            RenamePlayerUseCase(games, testClock),
            GameCommandRunner(
                games,
                setOf(
                    RankingMessageEngine(messages, {
                        MessageLibrary.EMPTY
                    }, RankingMessageSelector(Random(1)), testClock),
                    soundEvents,
                ),
            ),
            music,
            soundEvents,
        )
        composeRule.setContent {
            DominoTheme {
                GameScreen(
                    onBack = {},
                    onGameClose = {},
                    onShowRoundResults = { roundShown = it },
                    onCelebrate = { celebrated = it },
                    onSeeAllMoves = {},
                    viewModel = viewModel,
                )
            }
        }
    }

    /** Opens the Add sheet of the player in [seat] and taps the quick amount [amount]. */
    private fun add(seat: Int, amount: Int) {
        composeRule.onAllNodesWithText("Add")[seat].performScrollTo().performClick()
        composeRule.onNodeWithText("+$amount").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun aScoreMovesTheRankingAndEndingTheRoundShowsTheResults() = runTest {
        StartGameUseCase(games, testClock)(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        showGame()

        add(seat = 1, amount = 7)

        assertEquals(listOf(0, 7), games.activeGame().players.map { it.score })
        composeRule.onAllNodesWithText("7")[0].assertIsDisplayed()
        composeRule.onAllNodesWithText("👑 KING", useUnmergedTree = true)[0].assertIsDisplayed()

        composeRule.onNodeWithText("End round").performClick()
        composeRule.waitForIdle()

        assertEquals(1, roundShown)
        assertEquals(2, games.activeGame().currentRound?.number)
    }

    @Test
    fun reachingTheTargetEndsTheGameWithTheConfirmedWinner() = runTest {
        StartGameUseCase(games, testClock)(targetScore = 10, playerNames = listOf("Omar", "Mona"))
        val gameId = games.activeGame().id
        showGame()

        add(seat = 0, amount = 10)
        composeRule.onNodeWithText("End game").performClick()
        composeRule.onNodeWithText("Who won?").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm winner").performClick()
        composeRule.waitForIdle()

        val game = games.game(gameId)
        assertEquals(GameStatus.COMPLETED, game.status)
        assertEquals("Omar", game.player(game.winnerId!!)?.name?.value)
        assertEquals(gameId.value, celebrated)
        assertNull(roundShown)
    }
}
