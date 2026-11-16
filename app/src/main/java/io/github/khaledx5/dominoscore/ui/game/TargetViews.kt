package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.domain.model.TargetScore
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing

/** Moving the target mid-game needs an explicit Change: a stray tap never changes who qualified. */
@Composable
fun TargetEditDialog(
    edit: TargetEditUi,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.target_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                // Raising the target means more rounds, and the table knows what that means.
                Text(
                    text = stringResource(R.string.target_dialog_joke),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.target_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = edit.text,
                    onValueChange = onTextChange,
                    label = { Text(stringResource(R.string.target_dialog_label)) },
                    suffix = { Text(pluralStringResource(R.plurals.points_unit, edit.text.toIntOrNull() ?: 0)) },
                    isError = edit.error != null,
                    supportingText = edit.error?.let { error -> { Text(targetErrorText(error)) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.target_dialog_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Shown while anyone is at or above the target: the round cannot be ended any more. */
@Composable
fun QualifiedBanner(names: List<String>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = DominoThemeExtras.gameColors.qualifiedContainer,
    ) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = pluralStringResource(R.plurals.game_qualified_title, names.size, names.joinToString()),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.game_qualified_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun targetErrorText(error: TargetEditError): String = when (error) {
    TargetEditError.NOT_A_NUMBER -> stringResource(R.string.error_target_not_a_number)
    TargetEditError.TOO_LOW -> stringResource(R.string.error_target_too_low, TargetScore.ALLOWED.first)
    TargetEditError.TOO_HIGH -> stringResource(R.string.error_target_too_high, TargetScore.ALLOWED.last)
    TargetEditError.UNCHANGED -> stringResource(R.string.error_target_unchanged)
}
