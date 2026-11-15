package io.github.khaledx5.dominoscore.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.Spacing

@Immutable
data class WinnerCandidateUi(val playerId: Long, val name: String, val score: Int)

/**
 * The open "who won?" dialog. With one leader they are pre-selected and any qualified player may
 * still be picked; with a tie at the top nothing is selected until the table picks one of them.
 */
@Immutable
data class WinnerPickUi(val candidates: List<WinnerCandidateUi>, val tieAtTop: Boolean, val selectedId: Long?) {
    val canConfirm: Boolean get() = selectedId != null
}

/** Nothing happens until Confirm: Cancel keeps the game going, with no music and no celebration. */
@Composable
fun WinnerDialog(
    pick: WinnerPickUi,
    onSelect: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.winner_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(
                    text = stringResource(
                        if (pick.tieAtTop) R.string.winner_dialog_tie else R.string.winner_dialog_body,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.selectableGroup()) {
                    pick.candidates.forEach { candidate ->
                        CandidateRow(
                            candidate = candidate,
                            selected = candidate.playerId == pick.selectedId,
                            onSelect = { onSelect(candidate.playerId) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = pick.canConfirm) {
                Text(stringResource(R.string.winner_dialog_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun CandidateRow(candidate: WinnerCandidateUi, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = candidate.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pluralStringResource(R.plurals.points_value, candidate.score, candidate.score),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview
@Composable
private fun WinnerDialogPreview() {
    DominoTheme {
        WinnerDialog(
            pick = WinnerPickUi(
                candidates = listOf(WinnerCandidateUi(1, "Omar", 160), WinnerCandidateUi(2, "Mona", 160)),
                tieAtTop = true,
                selectedId = null,
            ),
            onSelect = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
