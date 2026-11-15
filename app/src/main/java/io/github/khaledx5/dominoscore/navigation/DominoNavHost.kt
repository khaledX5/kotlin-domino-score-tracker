package io.github.khaledx5.dominoscore.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.khaledx5.dominoscore.ui.game.GameScreen
import io.github.khaledx5.dominoscore.ui.home.HomeScreen
import io.github.khaledx5.dominoscore.ui.setup.SetupScreen
import kotlinx.serialization.Serializable

/** Type-safe destinations: arguments are checked by the compiler instead of being built into route strings. */
@Serializable
data object HomeDestination

@Serializable
data object SetupDestination

@Serializable
data object GameDestination

@Composable
fun DominoNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HomeDestination, modifier = modifier) {
        composable<HomeDestination> {
            HomeScreen(
                onNewGame = { navController.navigate(SetupDestination) },
                onContinueGame = { navController.navigate(GameDestination) },
            )
        }
        composable<SetupDestination> {
            SetupScreen(
                onBack = { navController.popBackStack() },
                onGameStart = {
                    navController.navigate(GameDestination) { popUpTo(HomeDestination) }
                },
            )
        }
        composable<GameDestination> {
            GameScreen(
                onBack = { navController.popBackStack() },
                onGameClose = { navController.popBackStack(HomeDestination, inclusive = false) },
            )
        }
    }
}
