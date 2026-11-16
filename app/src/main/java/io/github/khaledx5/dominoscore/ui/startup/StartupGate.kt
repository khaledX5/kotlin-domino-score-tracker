package io.github.khaledx5.dominoscore.ui.startup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.khaledx5.dominoscore.ui.theme.Motion

/**
 * Shows [content] once startup is ready. Before that the system splash covers the window; if startup
 * runs past it, the [LoadingScreen] takes over and then fades out over the first screen. It sits
 * above navigation, so going back to home never shows it again.
 */
@Composable
fun StartupGate(state: StartupUiState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier) {
        if (state == StartupUiState.Ready) content()
        AnimatedVisibility(
            visible = state == StartupUiState.LoadingScreen,
            // The system splash fades out over it, so it needs no entrance of its own.
            enter = EnterTransition.None,
            exit = fadeOut(tween(Motion.QUICK_MS)),
        ) {
            LoadingScreen(ready = state == StartupUiState.Ready)
        }
    }
}
