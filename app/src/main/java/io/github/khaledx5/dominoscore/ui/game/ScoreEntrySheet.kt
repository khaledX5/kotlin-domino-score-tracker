package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.domain.model.ScoreAmount
import io.github.khaledx5.dominoscore.domain.model.ScoreOperationType
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing
import io.github.khaledx5.dominoscore.ui.theme.forNumbers

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
            // 1 to 10 in two even rows of five, for adding and for penalties alike.
            FlowRow(
                maxItemsInEachRow = QUICK_AMOUNTS_PER_ROW,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                ScoreEntryUi.QUICK_AMOUNTS.forEach { amount ->
                    QuickAmountTile(onClick = { confirm { onQuickAmount(amount) } }, modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                if (isPenalty) R.string.score_delta_penalty else R.string.score_delta_add,
                                amount,
                            ),
                            style = MaterialTheme.typography.titleMedium.forNumbers(),
                            color = accent,
                            maxLines = 1,
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

/** Like a choice tile, with less side padding so five fit in a row on a small phone. */
@Composable
private fun QuickAmountTile(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(modifier = Modifier.padding(vertical = Spacing.m), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

private const val QUICK_AMOUNTS_PER_ROW = 5

@Composable
private fun entryErrorText(error: ScoreEntryError): String = when (error) {
    ScoreEntryError.NOT_A_NUMBER -> stringResource(R.string.error_target_not_a_number)
    ScoreEntryError.TOO_LOW -> stringResource(R.string.error_amount_too_low, ScoreAmount.ALLOWED.first)
    ScoreEntryError.TOO_HIGH -> stringResource(R.string.error_amount_too_high, ScoreAmount.ALLOWED.last)
    ScoreEntryError.SCORE_LIMIT -> stringResource(R.string.error_score_limit)
}
