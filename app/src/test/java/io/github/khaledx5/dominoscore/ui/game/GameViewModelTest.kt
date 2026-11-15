package io.github.khaledx5.dominoscore.ui.game

import app.cash.turbine.test
import io.github.khaledx5.dominoscore.audio.MusicController
import io.github.khaledx5.dominoscore.audio.MusicTrack
import io.github.khaledx5.dominoscore.domain.messages.MessageCategory
import io.github.khaledx5.dominoscore.domain.messages.MessageLibrary
import io.github.khaledx5.dominoscore.domain.messages.MessageTone
import io.github.khaledx5.dominoscore.domain.messages.RankingMessage
import io.github.khaledx5.dominoscore.domain.messages.RankingMessageEngine
import io.github.khaledx5.dominoscore.domain.messages.RankingMessageSelector
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.domain.rules.PrimaryAction
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.testing.FakePlayerMessageRepository
import io.github.khaledx5.dominoscore.domain.usecase.ChangeTargetUseCase
import io.github.khaledx5.dominoscore.domain.usecase.EndRoundUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObserveActiveGameUseCase
import io.github.khaledx5.dominoscore.domain.usecase.ObservePlayerMessagesUseCase
import io.github.khaledx5.dominoscore.domain.usecase.RecordScoreUseCase
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.FakeMusicSettingsStore
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.RecordingAudio
import io.github.khaledx5.dominoscore.testing.testClock
import kotlin.random.Random
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
    private val recordingAudio = RecordingAudio()
    private val music = MusicController(
        audio = recordingAudio.manager,
        settingsStore = settings,
        observeActiveGame = ObserveActiveGameUseCase(games),
        scope = TestScope(mainDispatcherRule.dispatcher),
    )
    private val startGame = StartGameUseCase(games, testClock)
    private val recordScore = RecordScoreUseCase(games, testClock)

    private val messages = FakePlayerMessageRepository()
    private val library = MessageLibrary(
        MessageCategory.entries.flatMap { category ->
            (1..3).map { RankingMessage("${category.name.lowercase()}_$it", category, "line $it") }
        },
    )
    private val trashTalk = RankingMessageEngine(
        games,
        messages,
        { library },
        RankingMessageSelector(Random(1)),
        testClock,
    )

    private fun viewModel() = GameViewModel(
        ObserveActiveGameUseCase(games),
        ObservePlayerMessagesUseCase(messages, { library }),
        recordScore,
        ChangeTargetUseCase(games, testClock),
        EndRoundUseCase(games, testClock),
        trashTalk,
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
    fun theMusicButtonPlaysTheTune() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val viewModel = viewModel()

        viewModel.playMusic()

        assertEquals(listOf(MusicTrack.GAMEPLAY), recordingAudio.started)
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

            val after = expectMostRecentItem() as GameUiState.Playing
            assertEquals(2, after.game.roundNumber)
            assertEquals(40, after.game.players.first().score)
            // The screen opens the results of round 1 once, then the request is cleared.
            assertEquals(1, after.endedRound)

            viewModel.onRoundResultsShown()
            assertEquals(null, (awaitItem() as GameUiState.Playing).endedRound)
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

    @Test
    fun aScoreTapThatCrownsSomeoneSavesTheirMessages() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val viewModel = viewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            val omar = games.activeGame().players.first()
            viewModel.openScoreEntry(omar.id.value, omar.name.value, ScoreOperationType.ADD)
            viewModel.submitQuickAmount(10)

            assertEquals(
                listOf(MessageCategory.FIRST_TIME_KING, MessageCategory.FIRST_TIME_KOOZ),
                messages.all.map { it.category },
            )
            // Omar's card shows his KING line, Mona's her KOOZ line, straight from the library.
            val cards = expectMostRecentItem().game().players
            assertEquals(library.find(messages.all[0].messageId)?.text, cards[0].message?.text)
            assertEquals(MessageTone.KOOZ, cards[1].message?.tone)
        }
    }

    @Test
    fun collectingTheStateAgainNeverAddsMessages() = runTest {
        startGame(targetScore = 100, playerNames = listOf("Omar", "Mona"))
        val viewModel = viewModel()
        viewModel.onPrimaryAction(PrimaryAction.END_ROUND)
        val saved = messages.all.size

        // Re-collecting, a new screen instance and repeated emissions only read the state.
        repeat(3) { viewModel.uiState.test { expectMostRecentItem() } }
        viewModel().uiState.test { expectMostRecentItem() }

        assertEquals(2, saved)
        assertEquals(saved, messages.all.size)
    }
}
