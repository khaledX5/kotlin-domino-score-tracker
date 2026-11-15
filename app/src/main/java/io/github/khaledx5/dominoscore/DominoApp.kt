package io.github.khaledx5.dominoscore

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.khaledx5.dominoscore.navigation.DominoNavHost

@Composable
fun DominoApp(modifier: Modifier = Modifier) {
    DominoNavHost(modifier = modifier)
}
