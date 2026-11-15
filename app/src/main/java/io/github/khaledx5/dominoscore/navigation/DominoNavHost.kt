package io.github.khaledx5.dominoscore.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.khaledx5.dominoscore.ui.home.HomeScreen
import io.github.khaledx5.dominoscore.ui.setup.SetupScreen
import kotlinx.serialization.Serializable

/** Type-safe destinations: arguments are checked by the compiler instead of being built into route strings. */
@Serializable
data object HomeDestination

@Serializable
data object SetupDestination

@Composable
fun DominoNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HomeDestination, modifier = modifier) {
        composable<HomeDestination> {
            HomeScreen(onNewGame = { navController.navigate(SetupDestination) })
        }
        composable<SetupDestination> {
            // The game screen arrives with the scoring features; until then a new game lands back on home.
            SetupScreen(
                onBack = { navController.popBackStack() },
                onGameStart = { navController.popBackStack(HomeDestination, inclusive = false) },
            )
        }
    }
}
