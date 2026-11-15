package io.github.khaledx5.dominoscore.ui.game

import app.cash.turbine.test
import io.github.khaledx5.dominoscore.audio.MusicController
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.rules.PrimaryAction
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.ChangeTargetUseCase
import io.github.khaledx5.dominoscore.domain.usecase.EndRoundUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.FakeMusicSettingsStore
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.RecordingAudio
import io.github.khaledx5.dominoscore.testing.testClock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val settings = FakeMusicSettingsStore()
    private val music = MusicController(
        audio = RecordingAudio().manager,
        settingsStore = settings,
        observeActiveGame = ObserveActiveGameUseCase(games),
        scope = TestScope(mainDispatcherRule.dispatcher),
    )
    private val startGame = StartGameUseCase(games, testClock)
    private val recordScore = RecordScoreUseCase(games, testClock)

    private fun viewModel() = GameViewModel(
        ObserveActiveGameUseCase(games),
        recordScore,
        ChangeTargetUseCase(games, testClock),
        EndRoundUseCase(games, testClock),
        music,
    )

    @Test
    fun keepsCardsInSeatOrderAndTheRankingInScoreOrder() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona", "Ali"))
        recordScore(games.activeGame().players[2].id, ScoreOperationType.ADD, 30)

        viewModel().uiState.test {
            val game = (expectMostRecentItem() as GameUiState.Playing).game
            assertEquals(listOf("Omar", "Mona", "Ali"), game.players.map { it.name })
            assertEquals(listOf("Ali", "Omar", "Mona"), game.standings.map { it.name })
            assertTrue(game.players.last().isKing)
        }
    }

    @Test
    fun theMusicButtonShowsThePlayingState() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertFalse((expectMostRecentItem() as GameUiState.Playing).music.playing)

            viewModel.toggleMusic()

            assertTrue((awaitItem() as GameUiState.Playing).music.playing)
        }
    }

    @Test
    fun theMusicButtonIsHiddenWhenGameplayMusicIsOff() = runTest {
        settings.setGameplayEnabled(false)
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))

        viewModel().uiState.test {
            assertFalse((expectMostRecentItem() as GameUiState.Playing).music.visible)
        }
    }

    @Test
    fun reportsWhenThereIsNoGameAnyMore() = runTest {
        viewModel().uiState.test {
            assertEquals(GameUiState.NoActiveGame, expectMostRecentItem())
        }
    }

    private fun GameUiState.entry() = (this as GameUiState.Playing).scoreEntry

    private fun GameUiState.scores() = (this as GameUiState.Playing).game.players.map { it.score }

    @Test
    fun aQuickAmountIsSavedAtOnceAndClosesTheSheet() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val omar = games.activeGame().players.first()
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.openScoreEntry(omar.id.value, omar.name.value, ScoreOperationType.ADD)
            assertEquals("Omar", awaitItem().entry()?.playerName)

            viewModel.submitQuickAmount(15)

            val after = expectMostRecentItem()
            assertEquals(null, after.entry())
            assertEquals(listOf(15, 0), after.scores())
        }
    }

    @Test
    fun aTypedPenaltyCanTakeTheScoreBelowZero() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val mona = games.activeGame().players.last()
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.openScoreEntry(mona.id.value, mona.name.value, ScoreOperationType.PENALTY)
            viewModel.updateAmount("3a0")
            viewModel.submitAmount()

            assertEquals(listOf(0, -30), expectMostRecentItem().scores())
        }
    }

    @Test
    fun wrongAmountsStayInTheSheetWithAnError() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val omar = games.activeGame().players.first()
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.openScoreEntry(omar.id.value, omar.name.value, ScoreOperationType.ADD)

            viewModel.submitAmount()
            assertEquals(ScoreEntryError.NOT_A_NUMBER, expectMostRecentItem().entry()?.error)

            viewModel.updateAmount("0")
            viewModel.submitAmount()
            assertEquals(ScoreEntryError.TOO_LOW, expectMostRecentItem().entry()?.error)
            assertEquals(0, games.operations.size)
        }
    }

    @Test
    fun dismissingTheSheetSavesNothing() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val omar = games.activeGame().players.first()
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.openScoreEntry(omar.id.value, omar.name.value, ScoreOperationType.ADD)
            viewModel.updateAmount("20")
            viewModel.dismissScoreEntry()

            assertEquals(null, expectMostRecentItem().entry())
            assertEquals(0, games.operations.size)
        }
    }

    private fun GameUiState.game() = (this as GameUiState.Playing).game

    private fun GameUiState.targetEdit() = (this as GameUiState.Playing).targetEdit

    @Test
    fun aConfirmedTargetChangeIsSavedAndClosesTheDialog() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.openTargetEdit(100)
            assertEquals("100", awaitItem().targetEdit()?.text)

            viewModel.updateTargetText("250")
            viewModel.confirmTargetChange()

            val after = expectMostRecentItem()
            assertEquals(null, after.targetEdit())
            assertEquals(250, after.game().target)
        }
    }

    @Test
    fun cancellingTheTargetDialogChangesNothing() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.openTargetEdit(100)
            viewModel.updateTargetText("40")
            viewModel.dismissTargetEdit()

            assertEquals(100, expectMostRecentItem().game().target)
        }
    }

    @Test
    fun theSameTargetOrZeroStayInTheDialogWithAnError() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.openTargetEdit(100)
            viewModel.confirmTargetChange()
            assertEquals(TargetEditError.UNCHANGED, expectMostRecentItem().targetEdit()?.error)

            viewModel.updateTargetText("0")
            viewModel.confirmTargetChange()
            assertEquals(TargetEditError.TOO_LOW, expectMostRecentItem().targetEdit()?.error)
        }
    }

    @Test
    fun loweringTheTargetQualifiesPlayersForTheBanner() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        recordScore(games.activeGame().players.first().id, ScoreOperationType.ADD, 60)
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(emptyList<String>(), expectMostRecentItem().game().qualifiedNames)

            viewModel.openTargetEdit(100)
            viewModel.updateTargetText("50")
            viewModel.confirmTargetChange()

            assertEquals(listOf("Omar"), expectMostRecentItem().game().qualifiedNames)
        }
    }

    @Test
    fun endingTheRoundKeepsScoresAndStartsTheNextRound() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        recordScore(games.activeGame().players.first().id, ScoreOperationType.ADD, 40)
        val viewModel = viewModel()

        viewModel.uiState.test {
            val before = expectMostRecentItem().game()
            assertEquals(1, before.roundNumber)
            assertEquals(PrimaryAction.END_ROUND, before.primaryAction)

            viewModel.onPrimaryAction(PrimaryAction.END_ROUND)

            val after = expectMostRecentItem().game()
            assertEquals(2, after.roundNumber)
            assertEquals(40, after.players.first().score)
        }
    }

    @Test
    fun theMainButtonTurnsIntoEndGameOnceSomeoneQualifies() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(PrimaryAction.END_ROUND, expectMostRecentItem().game().primaryAction)

            recordScore(games.activeGame().players.first().id, ScoreOperationType.ADD, 100)
            assertEquals(PrimaryAction.END_GAME, expectMostRecentItem().game().primaryAction)

            // A stale End round tap after someone qualified is refused: the round stays open.
            viewModel.onPrimaryAction(PrimaryAction.END_ROUND)
            expectNoEvents()
            assertEquals(1, games.activeGame().currentRound?.number)
        }
    }
}
