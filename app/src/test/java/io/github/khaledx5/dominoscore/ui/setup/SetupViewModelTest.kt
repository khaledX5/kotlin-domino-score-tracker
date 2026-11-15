package io.github.khaledx5.dominoscore.ui.setup

import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.testing.FakeGameRepository
import io.github.khaledx5.dominoscore.domain.usecase.StartGameUseCase
import io.github.khaledx5.dominoscore.testing.MainDispatcherRule
import io.github.khaledx5.dominoscore.testing.testClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeGameRepository()
    private val viewModel = SetupViewModel(StartGameUseCase(repository, testClock))

    private val state get() = viewModel.uiState.value

    private fun typeNames(vararg names: String) = names.forEachIndexed(viewModel::updateName)

    @Test
    fun startsWithTwoPlayersAndTheDefaultTarget() {
        assertEquals(TargetChoice.Preset(SetupUiState.DEFAULT_TARGET), state.targetChoice)
        assertEquals(2, state.playerCount)
        assertEquals(listOf("", ""), state.visibleNames)
    }

    @Test
    fun startsTheGameWithThePresetAndTheVisibleNamesOnly() = runTest {
        viewModel.selectPresetTarget(150)
        viewModel.selectPlayerCount(4)
        typeNames("Omar", "Mona", "Ali", "Sara")
        viewModel.selectPlayerCount(3)

        viewModel.start()

        assertTrue(state.gameStarted)
        val game = repository.activeGame()
        assertEquals(150, game.target.value)
        assertEquals(listOf("Omar", "Mona", "Ali"), game.players.map { it.name.value })
    }

    @Test
    fun keepsTypedNamesWhenThePlayerCountGoesDownAndUp() {
        viewModel.selectPlayerCount(4)
        typeNames("Omar", "Mona", "Ali", "Sara")

        viewModel.selectPlayerCount(2)
        viewModel.selectPlayerCount(4)

        assertEquals(listOf("Omar", "Mona", "Ali", "Sara"), state.visibleNames)
    }

    @Test
    fun usesTheCustomTargetAndKeepsOnlyDigits() = runTest {
        viewModel.selectCustomTarget()
        viewModel.updateCustomTarget("2a5-0")
        typeNames("Omar", "Mona")

        viewModel.start()

        assertEquals("250", state.customTarget)
        assertEquals(250, repository.activeGame().target.value)
    }

    @Test
    fun anEmptyCustomTargetIsNotANumber() = runTest {
        viewModel.selectCustomTarget()
        typeNames("Omar", "Mona")

        viewModel.start()

        assertEquals(TargetFieldError.NOT_A_NUMBER, state.targetError)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun aZeroCustomTargetIsTooLowAndTypingClearsTheError() = runTest {
        viewModel.selectCustomTarget()
        viewModel.updateCustomTarget("0")
        typeNames("Omar", "Mona")

        viewModel.start()
        assertEquals(TargetFieldError.TOO_LOW, state.targetError)

        viewModel.updateCustomTarget("75")
        assertNull(state.targetError)
    }

    @Test
    fun marksBlankAndDuplicateNamesOnTheirFields() = runTest {
        viewModel.selectPlayerCount(4)
        typeNames("Omar", " ", "omar", "Sara")

        viewModel.start()

        assertEquals(
            mapOf(0 to NameFieldError.DUPLICATE, 1 to NameFieldError.BLANK, 2 to NameFieldError.DUPLICATE),
            state.nameErrors,
        )
        assertFalse(state.gameStarted)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun editingANameClearsOnlyItsError() = runTest {
        typeNames("", "")
        viewModel.start()

        viewModel.updateName(0, "Omar")

        assertEquals(mapOf(1 to NameFieldError.BLANK), state.nameErrors)
    }

    @Test
    fun asksBeforeReplacingAGameInProgress() = runTest {
        StartGameUseCase(repository, testClock)(targetScore = 100, playerNames = listOf("Ali", "Sara"))
        val oldGame = repository.activeGame().id
        typeNames("Omar", "Mona")

        viewModel.start()
        assertTrue(state.confirmReplaceActiveGame)
        assertFalse(state.gameStarted)

        viewModel.confirmReplaceActiveGame()

        assertTrue(state.gameStarted)
        assertFalse(state.confirmReplaceActiveGame)
        assertEquals(GameStatus.ABANDONED, repository.game(oldGame).status)
        assertEquals(listOf("Omar", "Mona"), repository.activeGame().players.map { it.name.value })
    }

    @Test
    fun cancellingTheReplacementKeepsTheGameInProgress() = runTest {
        StartGameUseCase(repository, testClock)(targetScore = 100, playerNames = listOf("Ali", "Sara"))
        typeNames("Omar", "Mona")
        viewModel.start()

        viewModel.dismissReplaceActiveGame()

        assertFalse(state.confirmReplaceActiveGame)
        assertEquals(listOf("Ali", "Sara"), repository.activeGame().players.map { it.name.value })
    }

    @Test
    fun theNavigationEventIsConsumedOnce() = runTest {
        typeNames("Omar", "Mona")
        viewModel.start()

        viewModel.onGameStartedHandled()

        assertFalse(state.gameStarted)
    }
}
