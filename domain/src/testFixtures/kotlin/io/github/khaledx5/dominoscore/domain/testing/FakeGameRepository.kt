package io.github.khaledx5.dominoscore.domain.testing

import io.github.khaledx5.dominoscore.domain.history.CompletedGame
import io.github.khaledx5.dominoscore.domain.history.FinalStanding
import io.github.khaledx5.dominoscore.domain.history.ScoreMove
import io.github.khaledx5.dominoscore.domain.messages.ScoreLog
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.GameId
import io.github.khaledx5.dominoscore.domain.model.GameStatus
import io.github.khaledx5.dominoscore.domain.model.Player
import io.github.khaledx5.dominoscore.domain.model.PlayerId
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.Profile
import io.github.khaledx5.dominoscore.domain.model.ProfileId
import io.github.khaledx5.dominoscore.domain.model.Round
import io.github.khaledx5.dominoscore.domain.model.RoundId
import io.github.khaledx5.dominoscore.domain.model.RoundStatus
import io.github.khaledx5.dominoscore.domain.model.ScoreOperation
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.domain.repository.GameRepository
import io.github.khaledx5.dominoscore.domain.repository.NewGame
import io.github.khaledx5.dominoscore.domain.rules.RankedPlayer
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
    private val moves = MutableStateFlow<List<Pair<GameId, ScoreMove>>>(emptyList())
    val finalStandings = mutableMapOf<GameId, List<RankedPlayer>>()
    private val frozen = MutableStateFlow<Map<GameId, List<RankedPlayer>>>(emptyMap())

    /** Profiles by name key, newest last; a profile seen again moves to the end. */
    private val profiles = MutableStateFlow<List<Pair<String, Profile>>>(emptyList())
    private val profileByPlayer = mutableMapOf<PlayerId, Profile>()

    fun profileOf(playerId: PlayerId): ProfileId = profileByPlayer.getValue(playerId).id
    var writeCount = 0
        private set

    fun game(id: GameId): Game = games.value.single { it.id == id }

    fun allGames(): List<Game> = games.value

    fun activeGame(): Game = games.value.single { it.isActive }

    /** Puts a game in storage directly, for tests that need a specific state. */
    fun seed(game: Game) {
        game.players.forEach { profileByPlayer.getOrPut(it.id) { profileFor(it.name) } }
        games.update { it + game }
        nextId = maxOf(nextId, game.id.value + 1, game.players.maxOf { it.id.value } + 1)
    }

    override fun observeActiveGame(): Flow<Game?> =
        games.map { list -> list.firstOrNull { it.isActive } }.distinctUntilChanged()

    override suspend fun findActiveGame(): Game? = games.value.firstOrNull { it.isActive }

    override fun observeCompletedGames(): Flow<List<CompletedGame>> = combine(games, frozen) { list, standings ->
        list.filter { it.status == GameStatus.COMPLETED }
            .sortedWith(compareByDescending<Game> { it.completedAt }.thenByDescending { it.id.value })
            .map { game -> completedGame(game, standings.getValue(game.id)) }
    }.distinctUntilChanged()

    override fun observeProfiles(): Flow<List<Profile>> =
        profiles.map { list -> list.map { it.second }.reversed() }.distinctUntilChanged()

    override suspend fun findGame(gameId: GameId): Game? = games.value.firstOrNull { it.id == gameId }

    override fun observeMoves(gameId: GameId): Flow<List<ScoreMove>> = moves
        .map { all -> all.filter { it.first == gameId }.map { it.second }.reversed() }
        .distinctUntilChanged()

    override suspend fun findScoreLog(gameId: GameId): ScoreLog =
        ScoreLog(roundsByGame[gameId].orEmpty().toList(), operationsByGame[gameId].orEmpty().toList())

    override suspend fun startGame(newGame: NewGame): GameId {
        writeCount++
        val game = Game(
            id = GameId(nextId++),
            target = newGame.target,
            status = GameStatus.ACTIVE,
            players = newGame.playerNames.mapIndexed { seat, name ->
                newPlayer(seat, name, newGame.colorIndexes[seat]).also { profileByPlayer[it.id] = profileFor(name) }
            },
            currentRound = newRound(number = 1, startedAt = newGame.startedAt),
            startedAt = newGame.startedAt,
        )
        games.update { list -> list.map(::abandonIfActive) + game }
        return game.id
    }

    override suspend fun recordScore(gameId: GameId, operation: ScoreOperation) = write(gameId) { game ->
        operations += operation
        operationsByGame.getOrPut(gameId) { mutableListOf() } += operation
        val move = ScoreMove(
            id = (moves.value.size + 1).toLong(),
            roundNumber = checkNotNull(game.currentRound).number,
            playerId = operation.playerId,
            type = operation.type,
            amount = operation.amount.value,
            newScore = operation.newScore,
            createdAt = operation.createdAt,
        )
        moves.update { it + (gameId to move) }
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

    override suspend fun completeGame(
        gameId: GameId,
        winnerId: PlayerId,
        finalStandings: List<RankedPlayer>,
        completedAt: Instant,
    ) = write(gameId) { game ->
        check(finalStandings.map { it.player.id }.toSet() == game.players.map { it.id }.toSet()) {
            "Final standings must cover every player of game $gameId"
        }
        this.finalStandings[gameId] = finalStandings
        frozen.update { it + (gameId to finalStandings) }
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
            profileByPlayer[playerId] = profileFor(newName)
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

    private fun profileFor(name: PlayerName): Profile {
        val known = profiles.value.firstOrNull { it.first == name.key }?.second
        val profile = known ?: Profile(ProfileId(nextId++), name)
        profiles.update { list -> list.filterNot { it.first == name.key } + (name.key to profile) }
        return profile
    }

    private fun completedGame(game: Game, standings: List<RankedPlayer>) = CompletedGame(
        id = game.id,
        target = game.target,
        startedAt = game.startedAt,
        completedAt = checkNotNull(game.completedAt),
        roundCount = roundsByGame[game.id].orEmpty().size,
        winnerId = checkNotNull(game.winnerId),
        standings = standings.map { ranked ->
            val player = checkNotNull(game.player(ranked.player.id))
            FinalStanding(
                playerId = player.id,
                profile = profileByPlayer.getValue(player.id),
                name = player.name,
                score = player.score,
                colorIndex = player.colorIndex,
                rank = ranked.rank,
                isKing = ranked.isKing,
                isKooz = ranked.isKooz,
            )
        },
    )

    private fun abandonIfActive(game: Game): Game =
        if (game.isActive) game.copy(status = GameStatus.ABANDONED, currentRound = null) else game

    private fun newPlayer(seat: Int, name: PlayerName, colorIndex: Int) =
        Player(id = PlayerId(nextId++), seat = seat, name = name, score = 0, colorIndex = colorIndex)

    private fun newRound(number: Int, startedAt: Instant) =
        Round(id = RoundId(nextId++), number = number, status = RoundStatus.IN_PROGRESS, startedAt = startedAt)
}
