package io.github.khaledx5.dominoscore.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing

/**
 * The colored dot in front of a name. It is a button: a ring around it says so, and its label
 * names the color and the seat for TalkBack.
 */
@Composable
internal fun ColorDot(colorIndex: Int, seat: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DominoThemeExtras.gameColors.players
    val colorName = stringArrayResource(R.array.player_color_names)[colorIndex]
    val description = stringResource(R.string.setup_color_button, seat + 1, colorName)
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.outline), CircleShape)
                .background(colors[colorIndex], CircleShape),
        )
    }
}

/** Only the colors no other player has taken are offered, with the player's own color marked. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorPickerDialog(
    seat: Int,
    current: Int,
    available: List<Int>,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = DominoThemeExtras.gameColors.players
    val names = stringArrayResource(R.array.player_color_names)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setup_color_title, seat + 1)) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                available.forEach { index ->
                    val selected = index == current
                    val ring = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    Surface(
                        onClick = { onPick(index) },
                        shape = CircleShape,
                        color = colors[index],
                        border = BorderStroke(
                            width = if (selected) 3.dp else 1.dp,
                            color = ring,
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = names[index] },
                    ) {
                        if (selected) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.setup_color_selected_mark),
                                    color = if (colors[index].luminance() > LIGHT_COLOR) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Above this the check mark is drawn dark, so it stays visible on lime or gold. */
private const val LIGHT_COLOR = 0.5f
