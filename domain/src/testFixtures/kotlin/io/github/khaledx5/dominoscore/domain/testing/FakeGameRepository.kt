package io.github.khaledx5.dominoscore.domain.testing

import io.github.khaledx5.dominoscore.domain.messages.ScoreLog
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.Round
import io.github.khaledx5.dominoscore.domain.model.RoundId
import io.github.khaledx5.dominoscore.domain.model.RoundStatus
import io.github.khaledx5.dominoscore.domain.model.ScoreOperation
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.repository.NewGame
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [GameRepository] that follows the documented contract, for use case tests. */
class FakeGameRepository : GameRepository {

    private val games = MutableStateFlow<List<Game>>(emptyList())
    private var nextId = 1L

    val operations = mutableListOf<ScoreOperation>()
    val completedRounds = mutableListOf<Round>()
    private val operationsByGame = mutableMapOf<GameId, MutableList<ScoreOperation>>()
    private val roundsByGame = mutableMapOf<GameId, MutableList<Round>>()
    var writeCount = 0
        private set

    fun game(id: GameId): Game = games.value.single { it.id == id }

    fun allGames(): List<Game> = games.value

    fun activeGame(): Game = games.value.single { it.isActive }

    /** Puts a game in storage directly, for tests that need a specific state. */
    fun seed(game: Game) {
        games.update { it + game }
        nextId = maxOf(nextId, game.id.value + 1, game.players.maxOf { it.id.value } + 1)
    }

    override fun observeActiveGame(): Flow<Game?> =
        games.map { list -> list.firstOrNull { it.isActive } }.distinctUntilChanged()

    override suspend fun findActiveGame(): Game? = games.value.firstOrNull { it.isActive }

    override suspend fun findGame(gameId: GameId): Game? = games.value.firstOrNull { it.id == gameId }

    override fun observeFinishedGames(): Flow<List<Game>> = games
        .map { list ->
            list.filterNot {
                it.isActive
            }.sortedWith(compareByDescending<Game> { it.startedAt }.thenByDescending { it.id.value })
        }
        .distinctUntilChanged()

    override suspend fun findScoreLog(gameId: GameId): ScoreLog =
        ScoreLog(roundsByGame[gameId].orEmpty().toList(), operationsByGame[gameId].orEmpty().toList())

    override suspend fun startGame(newGame: NewGame): GameId {
        writeCount++
        val game = Game(
            id = GameId(nextId++),
            target = newGame.target,
            status = GameStatus.ACTIVE,
            players = newGame.playerNames.mapIndexed { seat, name -> newPlayer(seat, name) },
            currentRound = newRound(number = 1, startedAt = newGame.startedAt),
            startedAt = newGame.startedAt,
        )
        games.update { list -> list.map(::abandonIfActive) + game }
        return game.id
    }

    override suspend fun recordScore(gameId: GameId, operation: ScoreOperation) = write(gameId) { game ->
        operations += operation
        operationsByGame.getOrPut(gameId) { mutableListOf() } += operation
        game.copy(
            players = game.players.map { if (it.id == operation.playerId) it.copy(score = operation.newScore) else it },
        )
    }

    override suspend fun changeTarget(gameId: GameId, newTarget: TargetScore, changedAt: Instant) =
        write(gameId) { it.copy(target = newTarget) }

    override suspend fun completeRound(gameId: GameId, roundId: RoundId, completedAt: Instant) = write(gameId) { game ->
        val round = finish(game, roundId, completedAt)
        game.copy(currentRound = newRound(number = round.number + 1, startedAt = completedAt))
    }

    override suspend fun completeGame(gameId: GameId, winnerId: PlayerId, completedAt: Instant) =
        write(gameId) { game ->
            finish(game, checkNotNull(game.currentRound).id, completedAt)
            game.copy(
                status = GameStatus.COMPLETED,
                winnerId = winnerId,
                currentRound = null,
                completedAt = completedAt,
            )
        }

    override suspend fun renamePlayer(gameId: GameId, playerId: PlayerId, newName: PlayerName, renamedAt: Instant) =
        write(gameId) { game ->
            game.copy(players = game.players.map { if (it.id == playerId) it.copy(name = newName) else it })
        }

    private fun write(gameId: GameId, change: (Game) -> Game) {
        writeCount++
        games.update { list -> list.map { if (it.id == gameId) change(it) else it } }
    }

    private fun finish(game: Game, roundId: RoundId, completedAt: Instant): Round {
        val round = checkNotNull(game.currentRound).also {
            check(it.id == roundId) { "Round $roundId is not in progress" }
        }
        return round.copy(status = RoundStatus.COMPLETED, completedAt = completedAt).also {
            completedRounds += it
            roundsByGame.getOrPut(game.id) { mutableListOf() } += it
        }
    }

    private fun abandonIfActive(game: Game): Game =
        if (game.isActive) game.copy(status = GameStatus.ABANDONED, currentRound = null) else game

    private fun newPlayer(seat: Int, name: PlayerName) =
        Player(id = PlayerId(nextId++), seat = seat, name = name, score = 0, colorIndex = seat)

    private fun newRound(number: Int, startedAt: Instant) =
        Round(id = RoundId(nextId++), number = number, status = RoundStatus.IN_PROGRESS, startedAt = startedAt)
}
