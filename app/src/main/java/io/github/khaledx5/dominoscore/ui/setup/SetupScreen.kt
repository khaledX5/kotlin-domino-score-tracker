package io.github.khaledx5.dominoscore.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.domain.model.Game
import io.github.khaledx5.dominoscore.domain.model.PlayerName
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.ui.components.ChoiceTile
import io.github.khaledx5.dominoscore.ui.components.SectionCard
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing

@Composable
fun SetupScreen(
    onBack: () -> Unit,
    onGameStart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnGameStart by rememberUpdatedState(onGameStart)

    LaunchedEffect(state.gameStarted) {
        if (state.gameStarted) {
            viewModel.onGameStartedHandled()
            currentOnGameStart()
        }
    }

    SetupContent(
        state = state,
        onBack = onBack,
        onSelectPreset = viewModel::selectPresetTarget,
        onSelectCustom = viewModel::selectCustomTarget,
        onCustomTargetChange = viewModel::updateCustomTarget,
        onPlayerCountChange = viewModel::selectPlayerCount,
        onNameChange = viewModel::updateName,
        onPickKnownPlayer = viewModel::pickKnownPlayer,
        onStart = viewModel::start,
        modifier = modifier,
    )

    if (state.confirmReplaceActiveGame) {
        ReplaceActiveGameDialog(
            onConfirm = viewModel::confirmReplaceActiveGame,
            onDismiss = viewModel::dismissReplaceActiveGame,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupContent(
    state: SetupUiState,
    onBack: () -> Unit,
    onSelectPreset: (Int) -> Unit,
    onSelectCustom: () -> Unit,
    onCustomTargetChange: (String) -> Unit,
    onPlayerCountChange: (Int) -> Unit,
    onNameChange: (seat: Int, name: String) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    onPickKnownPlayer: (String) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Button(
                onClick = onStart,
                enabled = !state.isStarting,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    // Stays above the gesture bar, and above the keyboard while a name is typed.
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = Spacing.xl, vertical = Spacing.l)
                    .height(Spacing.primaryButtonHeight),
            ) {
                if (state.isStarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.setup_start), style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            TargetSection(state, onSelectPreset, onSelectCustom, onCustomTargetChange)
            PlayerCountSection(playerCount = state.playerCount, onPlayerCountChange = onPlayerCountChange)
            PlayerNamesSection(state = state, onNameChange = onNameChange, onPickKnownPlayer = onPickKnownPlayer)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetSection(
    state: SetupUiState,
    onSelectPreset: (Int) -> Unit,
    onSelectCustom: () -> Unit,
    onCustomTargetChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(R.string.setup_target_title),
        iconRes = R.drawable.ic_flag,
        modifier = modifier,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            SetupUiState.TARGET_PRESETS.forEach { preset ->
                ChoiceTile(
                    selected = state.targetChoice == TargetChoice.Preset(preset),
                    onClick = { onSelectPreset(preset) },
                ) {
                    Text(text = preset.toString(), style = MaterialTheme.typography.titleMedium)
                }
            }
            ChoiceTile(selected = state.targetChoice == TargetChoice.Custom, onClick = onSelectCustom) {
                Text(text = stringResource(R.string.setup_target_custom), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (state.targetChoice == TargetChoice.Custom) {
            OutlinedTextField(
                value = state.customTarget,
                onValueChange = onCustomTargetChange,
                label = { Text(stringResource(R.string.setup_custom_target_label)) },
                suffix = { Text(stringResource(R.string.points_suffix)) },
                isError = state.targetError != null,
                supportingText = state.targetError?.let { error -> { Text(targetErrorText(error)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PlayerCountSection(playerCount: Int, onPlayerCountChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    SectionCard(
        title = stringResource(R.string.setup_players_title),
        iconRes = R.drawable.ic_group,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Game.PLAYER_COUNT.forEach { count ->
                ChoiceTile(
                    selected = count == playerCount,
                    onClick = { onPlayerCountChange(count) },
                    modifier = Modifier.weight(1f),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = count.toString(), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = pluralStringResource(R.plurals.setup_players_label, count),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerNamesSection(
    state: SetupUiState,
    onNameChange: (seat: Int, name: String) -> Unit,
    onPickKnownPlayer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerColors = DominoThemeExtras.gameColors.players
    SectionCard(
        title = stringResource(R.string.setup_names_title),
        iconRes = R.drawable.ic_person,
        modifier = modifier,
    ) {
        state.visibleNames.forEachIndexed { seat, name ->
            val error = state.nameErrors[seat]
            OutlinedTextField(
                value = name,
                onValueChange = { onNameChange(seat, it) },
                placeholder = { Text(stringResource(R.string.setup_player_placeholder, seat + 1)) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(playerColors[seat % playerColors.size], CircleShape),
                    )
                },
                isError = error != null,
                supportingText = error?.let { { Text(nameErrorText(it)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = if (seat == state.playerCount - 1) ImeAction.Done else ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.suggestedPlayers.isNotEmpty()) {
            KnownPlayers(
                names = state.suggestedPlayers,
                enabled = state.firstEmptySeat != null,
                onPick = onPickKnownPlayer,
            )
        }
    }
}

/** People from earlier games: one tap writes the name into the first empty field, spelled the same way. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KnownPlayers(names: List<String>, enabled: Boolean, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = stringResource(R.string.setup_known_players),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            names.forEach { name ->
                SuggestionChip(
                    onClick = { onPick(name) },
                    label = { Text(name) },
                    enabled = enabled,
                    shape = MaterialTheme.shapes.small,
                    // The default label grey reads as disabled on the light card.
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ReplaceActiveGameDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setup_replace_title)) },
        text = { Text(stringResource(R.string.setup_replace_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.setup_replace_confirm), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun targetErrorText(error: TargetFieldError): String = when (error) {
    TargetFieldError.NOT_A_NUMBER -> stringResource(R.string.error_target_not_a_number)
    TargetFieldError.TOO_LOW -> stringResource(R.string.error_target_too_low, TargetScore.ALLOWED.first)
    TargetFieldError.TOO_HIGH -> stringResource(R.string.error_target_too_high, TargetScore.ALLOWED.last)
}

@Composable
internal fun nameErrorText(error: NameFieldError): String = when (error) {
    NameFieldError.BLANK -> stringResource(R.string.error_name_blank)

    NameFieldError.TOO_LONG ->
        pluralStringResource(R.plurals.error_name_too_long, PlayerName.MAX_LENGTH, PlayerName.MAX_LENGTH)

    NameFieldError.DUPLICATE -> stringResource(R.string.error_name_duplicate)
}

@Preview
@Composable
private fun SetupContentPreview() {
    DominoTheme {
        SetupContent(
            state = SetupUiState(
                targetChoice = TargetChoice.Custom,
                customTarget = "0",
                targetError = TargetFieldError.TOO_LOW,
                playerCount = 3,
                names = listOf("Omar", "omar", "", ""),
                nameErrors = mapOf(0 to NameFieldError.DUPLICATE, 1 to NameFieldError.DUPLICATE),
                knownPlayers = listOf("Omar", "Mona", "Ali"),
            ),
            onBack = {},
            onSelectPreset = {},
            onSelectCustom = {},
            onCustomTargetChange = {},
            onPlayerCountChange = {},
            onNameChange = { _, _ -> },
            onStart = {},
        )
    }
}
