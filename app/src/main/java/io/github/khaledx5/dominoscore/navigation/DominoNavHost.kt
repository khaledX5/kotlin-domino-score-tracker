package io.github.khaledx5.dominoscore.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.khaledx5.dominoscore.ui.home.HomeScreen
import kotlinx.serialization.Serializable

/** Type-safe destinations: arguments are checked by the compiler instead of being built into route strings. */
@Serializable
data object HomeDestination

@Composable
fun DominoNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HomeDestination, modifier = modifier) {
        composable<HomeDestination> {
            HomeScreen()
        }
    }
}
