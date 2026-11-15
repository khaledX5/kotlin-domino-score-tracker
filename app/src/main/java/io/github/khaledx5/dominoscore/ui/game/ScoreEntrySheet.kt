package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.domain.model.ScoreAmount
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.ui.components.ChoiceTile
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing

/** Add or Penalty for one player: a quick amount saves in one tap, or any amount can be typed. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScoreEntrySheet(
    entry: ScoreEntryUi,
    onAmountChange: (String) -> Unit,
    onQuickAmount: (Int) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val isPenalty = entry.type == ScoreOperationType.PENALTY
    val gameColors = DominoThemeExtras.gameColors
    val accent = if (isPenalty) gameColors.penalty else gameColors.add
    val confirm = { action: () -> Unit ->
        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        action()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Text(
                text = stringResource(
                    if (isPenalty) R.string.score_sheet_penalty_title else R.string.score_sheet_add_title,
                    entry.playerName,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                ScoreEntryUi.QUICK_AMOUNTS.forEach { amount ->
                    ChoiceTile(selected = false, onClick = { confirm { onQuickAmount(amount) } }) {
                        Text(
                            text = stringResource(
                                if (isPenalty) R.string.score_delta_penalty else R.string.score_delta_add,
                                amount,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = accent,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = entry.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.score_sheet_custom_label)) },
                isError = entry.error != null,
                supportingText = entry.error?.let { error -> { Text(entryErrorText(error)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { confirm(onSubmit) }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { confirm(onSubmit) },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.primaryButtonHeight),
            ) {
                Text(stringResource(R.string.score_sheet_save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun entryErrorText(error: ScoreEntryError): String = when (error) {
    ScoreEntryError.NOT_A_NUMBER -> stringResource(R.string.error_target_not_a_number)
    ScoreEntryError.TOO_LOW -> stringResource(R.string.error_amount_too_low, ScoreAmount.ALLOWED.first)
    ScoreEntryError.TOO_HIGH -> stringResource(R.string.error_amount_too_high, ScoreAmount.ALLOWED.last)
    ScoreEntryError.SCORE_LIMIT -> stringResource(R.string.error_score_limit)
}
