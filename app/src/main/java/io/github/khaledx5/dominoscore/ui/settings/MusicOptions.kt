package io.github.khaledx5.dominoscore.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.audio.MusicSettings
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.Spacing

@Composable
fun MusicOptions(
    settings: MusicSettings,
    onGameplayMusicChange: (Boolean) -> Unit,
    onWinnerMusicChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = stringResource(R.string.settings_music),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SwitchRow(
            title = stringResource(R.string.settings_gameplay_music),
            hint = stringResource(R.string.settings_gameplay_music_hint),
            checked = settings.gameplayEnabled,
            onCheckedChange = onGameplayMusicChange,
        )
        SwitchRow(
            title = stringResource(R.string.settings_winner_music),
            hint = stringResource(R.string.settings_winner_music_hint),
            checked = settings.winnerEnabled,
            onCheckedChange = onWinnerMusicChange,
        )
        VolumeSlider(volume = settings.volume, onVolumeChange = onVolumeChange)
    }
}

@Composable
internal fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // The whole row toggles, so the switch itself takes no clicks.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Saves the volume when the finger lifts, not on every step of the drag. */
@Composable
private fun VolumeSlider(volume: Float, onVolumeChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    var dragged by remember(volume) { mutableFloatStateOf(volume) }
    val label = stringResource(R.string.settings_volume)
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = dragged,
            onValueChange = { dragged = it },
            onValueChangeFinished = { onVolumeChange(dragged) },
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MusicOptionsPreview() {
    DominoTheme {
        MusicOptions(
            settings = MusicSettings(winnerEnabled = false),
            onGameplayMusicChange = {},
            onWinnerMusicChange = {},
            onVolumeChange = {},
        )
    }
}
