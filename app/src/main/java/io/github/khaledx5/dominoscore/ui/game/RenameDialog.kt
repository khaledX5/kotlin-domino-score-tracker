package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.setup.NameFieldError
import io.github.khaledx5.dominoscore.ui.setup.nameErrorText

/** The open rename dialog. Only the name changes: the player id, score and history stay. */
@Immutable
data class RenameUi(val playerId: Long, val currentName: String, val text: String, val error: NameFieldError? = null)

@Composable
fun RenameDialog(
    rename: RenameUi,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.rename_title, rename.currentName)) },
        text = {
            OutlinedTextField(
                value = rename.text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.rename_label)) },
                isError = rename.error != null,
                supportingText = rename.error?.let { error -> { Text(nameErrorText(error)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.rename_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
