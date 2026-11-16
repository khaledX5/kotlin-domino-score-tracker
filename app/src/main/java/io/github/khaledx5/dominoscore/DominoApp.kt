package io.github.khaledx5.dominoscore

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.khaledx5.dominoscore.navigation.DominoNavHost
import io.github.khaledx5.dominoscore.ui.startup.StartupUiState

@Composable
fun DominoApp(startupState: StartupUiState, modifier: Modifier = Modifier) {
    // Until startup is ready the system splash covers the window, so there is nothing to draw yet.
    if (startupState == StartupUiState.Ready) {
        DominoNavHost(modifier = modifier)
    }
}
