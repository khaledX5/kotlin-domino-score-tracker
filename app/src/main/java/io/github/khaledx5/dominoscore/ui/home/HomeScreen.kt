package io.github.khaledx5.dominoscore.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.settings.SettingsSheet
import io.github.khaledx5.dominoscore.ui.theme.Spacing

@Composable
fun HomeScreen(
    onNewGame: () -> Unit,
    onContinueGame: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    HomeContent(
        state = state,
        onNewGame = onNewGame,
        onContinueGame = onContinueGame,
        onOpenSettings = { showSettings = true },
        modifier = modifier,
        onOpenHistory = onOpenHistory,
    )
    if (showSettings) {
        SettingsSheet(onDismiss = { showSettings = false })
    }
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onNewGame: () -> Unit,
    onContinueGame: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenHistory: () -> Unit = {},
) {
    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.settings_open),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            HomeHeader()
            when (state) {
                HomeUiState.Loading -> CircularProgressIndicator()
                is HomeUiState.Ready -> state.activeGame?.let { ActiveGameCard(game = it, onContinue = onContinueGame) }
            }
            Button(
                onClick = onNewGame,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.primaryButtonHeight),
            ) {
                Text(stringResource(R.string.home_new_game), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
                onClick = onOpenHistory,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.primaryButtonHeight),
            ) {
                Text(stringResource(R.string.home_history), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
