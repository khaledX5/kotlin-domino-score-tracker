package io.github.khaledx5.dominoscore

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.khaledx5.dominoscore.navigation.DominoNavHost
import io.github.khaledx5.dominoscore.ui.startup.StartupGate
import io.github.khaledx5.dominoscore.ui.startup.StartupUiState

@Composable
fun DominoApp(startupState: StartupUiState, modifier: Modifier = Modifier) {
    StartupGate(state = startupState, modifier = modifier.fillMaxSize()) {
        DominoNavHost()
    }
}
